package com.boutique.order;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Statistiques Hibernate pour compter les requêtes, et SQL affiché dans les logs du test
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "spring.jpa.properties.hibernate.generate_statistics=true",
		"logging.level.org.hibernate.SQL=debug",
		"logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=warn" })
class OrderServiceIntegrationTests {

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private Statistics statistics;

	private Long userId;

	private Long boubouId;

	private Long sandalesId;

	// Données de départ : 1 cliente, 2 produits (stock 10), 3 commandes de 2 lignes chacune
	@BeforeEach
	void setUp() {
		statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		boubouId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 10))
			.getId();
		sandalesId = productRepository.save(new Product(mode, "Sandales cuir", null, new BigDecimal("8000"), 10))
			.getId();
		userId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		for (int i = 0; i < 3; i++) {
			orderService.createOrder(userId,
					List.of(new OrderLineRequest(boubouId, 1), new OrderLineRequest(sandalesId, 1)));
		}
	}

	@AfterEach
	void cleanUp() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void naiveQueryTriggersOneExtraQueryPerOrder() {
		statistics.clear();

		transactionTemplate.executeWithoutResult((status) -> orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
			.forEach((order) -> order.getItems().size()));

		// 1 requête pour les 3 commandes + 1 requête de lignes PAR commande = 4 : c'est le N+1
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + 3);
	}

	@Test
	void fetchJoinLoadsOrdersAndItemsInOneQuery() {
		statistics.clear();

		List<OrderResponse> orders = orderService.findOrdersOfUser(userId);

		assertThat(orders).hasSize(3).allSatisfy((order) -> assertThat(order.items()).hasSize(2));
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

	// Liste admin paginée : la page de commandes, puis les lignes de CES commandes. Sans ça : 1 + 3 requêtes.
	@Test
	void adminListIsPaginatedWithoutNPlusOne() {
		statistics.clear();

		var page = orderService.findAll(null, null, null, PageRequest.of(0, 10));

		assertThat(page.content()).hasSize(3).allSatisfy((order) -> assertThat(order.items()).hasSize(2));
		assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
	}

	@Test
	void createOrderUsesDatabasePriceAndRemovesStock() {
		OrderResponse order = orderService.createOrder(userId, List.of(new OrderLineRequest(boubouId, 2)));

		assertThat(order.totalAmount()).isEqualByComparingTo("50000");
		assertThat(stockOf(boubouId)).isEqualTo(10 - 3 - 2);
	}

	@Test
	void failureInTheMiddleRollsBackEverything() {
		long ordersBefore = orderRepository.count();

		// 1re ligne possible (baisse le stock du boubou), 2e ligne impossible : 50 sandales demandées, 7 en stock
		assertThatThrownBy(() -> orderService.createOrder(userId,
				List.of(new OrderLineRequest(boubouId, 2), new OrderLineRequest(sandalesId, 50))))
			.isInstanceOf(InsufficientStockException.class);

		assertThat(stockOf(boubouId)).isEqualTo(10 - 3);
		assertThat(orderRepository.count()).isEqualTo(ordersBefore);
	}

	private int stockOf(Long productId) {
		return productRepository.findById(productId).orElseThrow().getStock();
	}

}
