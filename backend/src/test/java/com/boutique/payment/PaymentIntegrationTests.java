package com.boutique.payment;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.boutique.TestcontainersConfiguration;
import com.boutique.catalog.Category;
import com.boutique.catalog.CategoryRepository;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductRepository;
import com.boutique.order.OrderLineRequest;
import com.boutique.order.OrderRepository;
import com.boutique.order.OrderService;
import com.boutique.order.OrderStatus;
import com.boutique.payment.PaymentProvider.ProviderStatus;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Paiement en ligne de bout en bout avec le faux fournisseur : seul un webhook VÉRIFIÉ marque la commande payée
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PaymentIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FakePaymentProvider fakeProvider;

	@Autowired
	private PaymentReconciliationJob reconciliationJob;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private PaymentEventRepository paymentEventRepository;

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

	private Long awaId;

	private Long moussaId;

	private Long orderId;

	@BeforeEach
	void setUp() {
		awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		moussaId = userRepository.save(new User("moussa@example.com", "Moussa Fall", Role.CUSTOMER)).getId();
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		Long boubouId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 10)).getId();
		orderId = orderService.createOrder(awaId, List.of(new OrderLineRequest(boubouId, 1))).id();
	}

	@AfterEach
	void cleanUp() {
		paymentEventRepository.deleteAll();
		paymentRepository.deleteAll();
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void orderIsPaidOnlyAfterAVerifiedWebhook() throws Exception {
		pay(awaId).andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PENDING"))
			.andExpect(jsonPath("$.amount").value(25000))
			.andExpect(jsonPath("$.checkoutUrl").value(containsString("/api/dev/fake-payments/FAKE-")));
		String reference = reference();

		// Le client a payé sur la page du fournisseur… mais tant que le webhook n'est pas arrivé, rien ne change
		fakeProvider.simulate(reference, ProviderStatus.SUCCEEDED);
		assertThat(orderStatus()).isEqualTo(OrderStatus.CREATED);

		webhook(reference, fakeProvider.sign(reference)).andExpect(status().isOk());

		assertThat(orderStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(payment().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
		assertThat(eventTypes()).containsExactly(PaymentEventType.INITIATED, PaymentEventType.WEBHOOK_RECEIVED,
				PaymentEventType.SUCCEEDED);
	}

	@Test
	void duplicateWebhookIsIgnored() throws Exception {
		pay(awaId).andExpect(status().isOk());
		String reference = reference();
		fakeProvider.simulate(reference, ProviderStatus.SUCCEEDED);

		webhook(reference, fakeProvider.sign(reference)).andExpect(status().isOk());
		webhook(reference, fakeProvider.sign(reference)).andExpect(status().isOk());

		assertThat(orderStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(eventTypes()).contains(PaymentEventType.DUPLICATE_IGNORED);
	}

	@Test
	void webhookWithInvalidSignatureIsRejectedAndLogged() throws Exception {
		pay(awaId).andExpect(status().isOk());
		String reference = reference();
		fakeProvider.simulate(reference, ProviderStatus.SUCCEEDED);

		webhook(reference, "signature-inventee").andExpect(status().isUnauthorized());

		assertThat(orderStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(payment().getStatus()).isEqualTo(PaymentStatus.PENDING);
		assertThat(paymentEventRepository.findAll()).extracting(PaymentEvent::getType)
			.contains(PaymentEventType.WEBHOOK_REJECTED);
	}

	@Test
	void paidAmountDifferentFromTheOrderIsRefused() throws Exception {
		pay(awaId).andExpect(status().isOk());
		String reference = reference();
		fakeProvider.simulate(reference, ProviderStatus.SUCCEEDED, new BigDecimal("100"));

		webhook(reference, fakeProvider.sign(reference)).andExpect(status().isOk());

		assertThat(orderStatus()).isEqualTo(OrderStatus.CREATED);
		assertThat(payment().getStatus()).isEqualTo(PaymentStatus.FAILED);
		assertThat(eventTypes()).contains(PaymentEventType.AMOUNT_MISMATCH);
	}

	@Test
	void cancelledPaymentLeavesTheOrderUnpaidAndTheCustomerCanRetry() throws Exception {
		pay(awaId).andExpect(status().isOk());
		String reference = reference();
		fakeProvider.simulate(reference, ProviderStatus.FAILED);

		webhook(reference, fakeProvider.sign(reference)).andExpect(status().isOk());

		assertThat(orderStatus()).isEqualTo(OrderStatus.CREATED);
		pay(awaId).andExpect(status().isOk());
		assertThat(paymentRepository.findByOrderId(orderId)).hasSize(2);
	}

	// Le webhook s'est perdu : la réconciliation redemande le statut au fournisseur et rattrape le paiement
	@Test
	void reconciliationCatchesAMissedWebhook() throws Exception {
		pay(awaId).andExpect(status().isOk());
		fakeProvider.simulate(reference(), ProviderStatus.SUCCEEDED);

		reconciliationJob.reconcilePendingPayments();

		assertThat(orderStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(eventTypes()).contains(PaymentEventType.RECONCILED, PaymentEventType.SUCCEEDED);
	}

	@Test
	void customerCannotPaySomeoneElsesOrder() throws Exception {
		pay(moussaId).andExpect(status().isNotFound());
	}

	@Test
	void paidOrderCannotBePaidAgain() throws Exception {
		pay(awaId).andExpect(status().isOk());
		String reference = reference();
		fakeProvider.simulate(reference, ProviderStatus.SUCCEEDED);
		webhook(reference, fakeProvider.sign(reference)).andExpect(status().isOk());

		pay(awaId).andExpect(status().isConflict());
	}

	@Test
	void webhookForAnUnknownProviderIsRefused() throws Exception {
		mockMvc.perform(post("/api/payments/webhooks/stripe").param("reference", "x")).andExpect(status().isNotFound());
	}

	private ResultActions pay(Long userId) throws Exception {
		return mockMvc.perform(post("/api/orders/{id}/payment", orderId).with(customer(userId)));
	}

	// Le fournisseur appelle l'API sans jeton : seule sa signature prouve que la requête vient de lui
	private ResultActions webhook(String reference, String signature) throws Exception {
		return mockMvc.perform(post("/api/payments/webhooks/fake").contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("reference", reference)
			.header(FakePaymentProvider.SIGNATURE_HEADER, signature));
	}

	private Payment payment() {
		return paymentRepository.findByOrderId(orderId).get(0);
	}

	private String reference() {
		return payment().getProviderReference();
	}

	private OrderStatus orderStatus() {
		return orderRepository.findById(orderId).orElseThrow().getStatus();
	}

	private List<PaymentEventType> eventTypes() {
		return paymentEventRepository.findByPaymentIdOrderByIdAsc(payment().getId())
			.stream()
			.map(PaymentEvent::getType)
			.toList();
	}

	private static RequestPostProcessor customer(Long userId) {
		return jwt().jwt((token) -> token.issuer("boutique-api").subject(userId.toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
	}

}
