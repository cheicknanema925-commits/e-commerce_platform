package com.boutique.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import com.boutique.TestcontainersConfiguration;
import com.boutique.catalog.Category;
import com.boutique.catalog.CategoryRepository;
import com.boutique.catalog.InsufficientStockException;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductRepository;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

// Deux clients, un seul boubou en stock, au même instant : grâce à @Version, un seul peut l'avoir
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConcurrentPurchaseIntegrationTests {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderService orderService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private Long lastBoubouId;

	@BeforeEach
	void setUp() {
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		lastBoubouId = productRepository.save(new Product(mode, "Dernier boubou", null, new BigDecimal("25000"), 1)).getId();
	}

	@AfterEach
	void cleanUp() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	// Le mécanisme pas à pas : les deux transactions lisent stock = 1 (version 0), PUIS les deux écrivent.
	// La 1re écrit « stock = 0, version = 1 WHERE version = 0 ». La 2de ne trouve plus de ligne en version 0 → échec.
	@Test
	void secondOfTwoSimultaneousStockUpdatesIsRejected() throws Exception {
		CyclicBarrier bothHaveRead = new CyclicBarrier(2);
		Callable<Void> buyer = () -> {
			transactionTemplate.executeWithoutResult((status) -> {
				Product product = productRepository.findById(lastBoubouId).orElseThrow();
				awaitQuietly(bothHaveRead);
				product.removeStock(1);
			});
			return null;
		};

		Outcome outcome = runTogether(buyer, buyer);

		assertThat(outcome.succeeded()).isEqualTo(1);
		assertThat(outcome.optimisticLockFailures()).isEqualTo(1);
		assertThat(productRepository.findById(lastBoubouId).orElseThrow().getStock()).isZero();
	}

	// Le parcours réel : deux commandes lancées en même temps. Selon le moment exact, le perdant reçoit soit
	// « stock insuffisant » (il a lu après le commit), soit un conflit de version. Dans tous les cas : 1 seule commande.
	@Test
	void onlyOneOfTwoCustomersGetsTheLastItem() throws Exception {
		Long awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		Long moussaId = userRepository.save(new User("moussa@example.com", "Moussa Fall", Role.CUSTOMER)).getId();
		CyclicBarrier start = new CyclicBarrier(2);
		List<OrderLineRequest> lastBoubou = List.of(new OrderLineRequest(lastBoubouId, 1));

		Outcome outcome = runTogether(() -> {
			awaitQuietly(start);
			orderService.createOrder(awaId, lastBoubou);
			return null;
		}, () -> {
			awaitQuietly(start);
			orderService.createOrder(moussaId, lastBoubou);
			return null;
		});

		assertThat(outcome.succeeded()).isEqualTo(1);
		assertThat(orderRepository.count()).isEqualTo(1);
		assertThat(productRepository.findById(lastBoubouId).orElseThrow().getStock()).isZero();
	}

	private static Outcome runTogether(Callable<Void> first, Callable<Void> second) throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			int succeeded = 0;
			int optimisticLockFailures = 0;
			for (Future<Void> result : executor.invokeAll(List.of(first, second), 30, TimeUnit.SECONDS)) {
				try {
					result.get();
					succeeded++;
				}
				catch (ExecutionException ex) {
					if (ex.getCause() instanceof OptimisticLockingFailureException) {
						optimisticLockFailures++;
					}
					else if (!(ex.getCause() instanceof InsufficientStockException)) {
						throw new IllegalStateException("Échec inattendu", ex.getCause());
					}
				}
			}
			return new Outcome(succeeded, optimisticLockFailures);
		}
		finally {
			executor.shutdownNow();
		}
	}

	private static void awaitQuietly(CyclicBarrier barrier) {
		try {
			barrier.await(10, TimeUnit.SECONDS);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private record Outcome(int succeeded, int optimisticLockFailures) {
	}

}
