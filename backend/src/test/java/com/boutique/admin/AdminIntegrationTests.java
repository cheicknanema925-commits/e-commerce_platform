package com.boutique.admin;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
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
import com.boutique.payment.Payment;
import com.boutique.payment.PaymentRepository;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Back-office : statistiques calculées par la base et statuts proposés par la machine à états
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdminIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserRepository userRepository;

	private Long paidOrderId;

	// Awa : 2 boubous payés en ligne (50 000 FCFA encaissés). Moussa : 1 paire de sandales, commande annulée.
	@BeforeEach
	void setUp() {
		Long awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		Long moussaId = userRepository.save(new User("moussa@example.com", "Moussa Fall", Role.CUSTOMER)).getId();
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		Long boubouId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 20)).getId();
		Long sandalesId = productRepository.save(new Product(mode, "Sandales cuir", null, new BigDecimal("8000"), 4)).getId();

		paidOrderId = orderService.createOrder(awaId, List.of(new OrderLineRequest(boubouId, 2))).id();
		Payment payment = new Payment(orderRepository.findById(paidOrderId).orElseThrow(), "fake", new BigDecimal("50000"),
				"REF-ADMIN-1");
		payment.succeed();
		paymentRepository.save(payment);
		orderService.markPaid(paidOrderId);

		Long cancelledOrderId = orderService.createOrder(moussaId, List.of(new OrderLineRequest(sandalesId, 1))).id();
		orderService.changeStatusAsAdmin(cancelledOrderId, OrderStatus.CANCELLED);
	}

	@AfterEach
	void cleanUp() {
		paymentRepository.deleteAll();
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void statsAreComputedByTheDatabase() throws Exception {
		mockMvc.perform(get("/api/admin/stats").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.collectedRevenue").value(50000))
			.andExpect(jsonPath("$.orderCount").value(2))
			.andExpect(jsonPath("$.customerCount").value(2))
			.andExpect(jsonPath("$.ordersByStatus[*].status").value(containsInAnyOrder("PAID", "CANCELLED")))
			// La commande annulée ne compte pas dans les meilleures ventes
			.andExpect(jsonPath("$.bestSellers.length()").value(1))
			.andExpect(jsonPath("$.bestSellers[0].name").value("Boubou bazin"))
			.andExpect(jsonPath("$.bestSellers[0].quantity").value(2))
			// Sandales : 4 en stock (≤ 5), remises en stock après l'annulation
			.andExpect(jsonPath("$.lowStock[0].name").value("Sandales cuir"))
			.andExpect(jsonPath("$.lowStock[0].stock").value(4));
	}

	@Test
	void adminOrderShowsTheCustomerAndTheAllowedNextStatuses() throws Exception {
		mockMvc.perform(get("/api/admin/orders/{id}", paidOrderId).with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.customerName").value("Awa Diop"))
			.andExpect(jsonPath("$.customerEmail").value("awa@example.com"))
			// Commande payée en ligne : l'admin peut la préparer ou l'annuler (remboursement), rien d'autre
			.andExpect(jsonPath("$.nextStatuses").value(containsInAnyOrder("PREPARING", "CANCELLED")));
	}

	@Test
	void customerCannotReadTheDashboard() throws Exception {
		mockMvc.perform(get("/api/admin/stats").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
			.andExpect(status().isForbidden());
	}

	private static RequestPostProcessor admin() {
		return jwt().jwt((token) -> token.issuer("boutique-api").subject("0"))
			.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

}
