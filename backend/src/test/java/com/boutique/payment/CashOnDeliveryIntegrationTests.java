package com.boutique.payment;

import java.math.BigDecimal;

import com.jayway.jsonpath.JsonPath;
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
import com.boutique.cart.CartRepository;
import com.boutique.catalog.Category;
import com.boutique.catalog.CategoryRepository;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductRepository;
import com.boutique.order.DeliveryRepository;
import com.boutique.order.OrderRepository;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Paiement en espèces à la livraison : commande préparée sans paiement, argent enregistré à la remise du colis
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CashOnDeliveryIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private DeliveryRepository deliveryRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private PaymentEventRepository paymentEventRepository;

	private Long awaId;

	private Long boubouId;

	@BeforeEach
	void setUp() {
		awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		boubouId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 10)).getId();
	}

	@AfterEach
	void cleanUp() {
		paymentEventRepository.deleteAll();
		paymentRepository.deleteAll();
		deliveryRepository.deleteAll();
		orderRepository.deleteAll();
		cartRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void cashOrderIsPreparedWithoutPaymentAndCashIsRecordedAtDelivery() throws Exception {
		Long orderId = checkout("CASH_ON_DELIVERY");

		// Pas de paiement en ligne pour une commande en espèces
		mockMvc.perform(post("/api/orders/{id}/payment", orderId).with(customer())).andExpect(status().isConflict());

		changeStatus(orderId, "PREPARING").andExpect(status().isOk());
		changeStatus(orderId, "SHIPPED").andExpect(status().isOk());
		assertThat(paymentRepository.findByOrderId(orderId)).isEmpty();

		changeStatus(orderId, "DELIVERED").andExpect(status().isOk());

		assertThat(paymentRepository.findByOrderId(orderId)).singleElement().satisfies((payment) -> {
			assertThat(payment.getProvider()).isEqualTo("cash");
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
			assertThat(payment.getAmount()).isEqualByComparingTo("25000");
		});
	}

	@Test
	void parcelRefusedAtTheDoorReturnsToStock() throws Exception {
		Long orderId = checkout("CASH_ON_DELIVERY");
		changeStatus(orderId, "PREPARING").andExpect(status().isOk());
		changeStatus(orderId, "SHIPPED").andExpect(status().isOk());
		assertThat(stock()).isEqualTo(9);

		changeStatus(orderId, "CANCELLED").andExpect(status().isOk());

		assertThat(stock()).isEqualTo(10);
		assertThat(paymentRepository.findByOrderId(orderId)).isEmpty();
	}

	@Test
	void onlineOrderIsNotPreparedBeforePayment() throws Exception {
		Long orderId = checkout("ONLINE");

		changeStatus(orderId, "PREPARING").andExpect(status().isConflict());
	}

	private Long checkout(String paymentMethod) throws Exception {
		mockMvc.perform(post("/api/cart/items").with(customer())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"productId\": %d, \"quantity\": 1}".formatted(boubouId))).andExpect(status().isOk());
		String body = mockMvc.perform(post("/api/orders").with(customer())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"delivery": {"addressLine": "Rue 10, Sicap Liberté 6", "city": "Dakar", "phone": "+221 77 123 45 67"},
					 "paymentMethod": "%s"}
					""".formatted(paymentMethod)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.paymentMethod").value(paymentMethod))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private ResultActions changeStatus(Long orderId, String status) throws Exception {
		return mockMvc.perform(patch("/api/admin/orders/{id}/status", orderId)
			.with(jwt().jwt((token) -> token.issuer("boutique-api").subject("0"))
				.authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"%s\"}".formatted(status)));
	}

	private int stock() {
		return productRepository.findById(boubouId).orElseThrow().getStock();
	}

	private RequestPostProcessor customer() {
		return jwt().jwt((token) -> token.issuer("boutique-api").subject(awaId.toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
	}

}
