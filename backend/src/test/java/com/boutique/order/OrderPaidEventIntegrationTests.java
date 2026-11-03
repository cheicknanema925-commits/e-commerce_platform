package com.boutique.order;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.boutique.TestcontainersConfiguration;
import com.boutique.catalog.Category;
import com.boutique.catalog.CategoryRepository;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductRepository;
import com.boutique.notification.NotificationSender;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// OrderPaidEvent : la notification part APRÈS le commit du paiement, et jamais si la transaction échoue
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderPaidEventIntegrationTests {

	// Le vrai envoi est remplacé par un mock pour vérifier s'il a été appelé
	@MockitoBean
	private NotificationSender notificationSender;

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

	private Long orderId;

	@BeforeEach
	void setUp() {
		Long awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		Long boubouId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 10)).getId();
		orderId = orderService.createOrder(awaId, List.of(new OrderLineRequest(boubouId, 1))).id();
	}

	@AfterEach
	void cleanUp() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void customerIsNotifiedOnceThePaymentIsCommitted() {
		orderService.markPaid(orderId);

		verify(notificationSender).send(eq("awa@example.com"), contains("Paiement confirmé"), contains("25000"));
	}

	// Le paiement est enregistré puis une panne survient dans la même transaction : tout est annulé,
	// et grâce à AFTER_COMMIT le client ne reçoit pas un faux « paiement confirmé »
	@Test
	void noNotificationWhenThePaymentTransactionIsRolledBack() {
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult((status) -> {
			orderService.markPaid(orderId);
			throw new IllegalStateException("panne juste après le paiement");
		})).isInstanceOf(IllegalStateException.class);

		verifyNoInteractions(notificationSender);
		assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREATED);
	}

	@Test
	void noNotificationForACancelledOrder() {
		orderService.changeStatusAsAdmin(orderId, OrderStatus.CANCELLED);

		assertThatThrownBy(() -> orderService.markPaid(orderId)).isInstanceOf(InvalidOrderTransitionException.class);
		verifyNoInteractions(notificationSender);
	}

}
