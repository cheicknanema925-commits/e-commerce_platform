package com.boutique.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.boutique.order.PaymentMethod;
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
			.andExpect(jsonPath("$.lowStock[0].stock").value(4))
			// Aujourd'hui et ce mois-ci : 50 000 encaissés, 1 commande (l'annulée ne compte pas)
			.andExpect(jsonPath("$.today.collected").value(50000))
			.andExpect(jsonPath("$.today.orders").value(1))
			.andExpect(jsonPath("$.thisMonth.collected").value(50000))
			.andExpect(jsonPath("$.awaitingConfirmation").value(0))
			// Stock au prix de vente : 18 boubous × 25 000 + 4 sandales × 8 000
			.andExpect(jsonPath("$.catalog.productCount").value(2))
			.andExpect(jsonPath("$.catalog.outOfStock").value(0))
			.andExpect(jsonPath("$.catalog.stockValue").value(482000))
			// Panier moyen : seule la commande payée compte (50 000), l'annulée est exclue
			.andExpect(jsonPath("$.averageOrderValue").value(50000))
			.andExpect(jsonPath("$.payments.collectedOnline").value(50000))
			.andExpect(jsonPath("$.payments.collectedCash").value(0))
			.andExpect(jsonPath("$.payments.cashToCollect").value(0))
			.andExpect(jsonPath("$.customers.newThisMonth").value(2))
			.andExpect(jsonPath("$.customers.repeat").value(0))
			.andExpect(jsonPath("$.salesByCategory.length()").value(1))
			.andExpect(jsonPath("$.salesByCategory[0].category").value("Mode"))
			.andExpect(jsonPath("$.salesByCategory[0].quantity").value(2))
			.andExpect(jsonPath("$.salesByCategory[0].revenue").value(50000));
	}

	// Espèces : à récupérer tant que la commande n'est pas livrée, puis comptées comme encaissées
	@Test
	void cashOnDeliveryIsToCollectUntilDeliveredThenCollected() throws Exception {
		Long awaId = userRepository.findByEmail("awa@example.com").orElseThrow().getId();
		Long boubouId = productRepository.findAll().stream().filter((p) -> p.getName().equals("Boubou bazin")).findFirst().orElseThrow().getId();
		Long cashOrderId = orderService.createOrder(awaId, List.of(new OrderLineRequest(boubouId, 1)), PaymentMethod.CASH_ON_DELIVERY).id();

		mockMvc.perform(get("/api/admin/stats").with(admin()))
			.andExpect(jsonPath("$.awaitingConfirmation").value(1))
			.andExpect(jsonPath("$.payments.cashToCollect").value(25000))
			.andExpect(jsonPath("$.payments.cashToCollectOrders").value(1))
			// Awa a maintenant 2 commandes non annulées : cliente fidèle
			.andExpect(jsonPath("$.customers.repeat").value(1));

		orderService.changeStatusAsAdmin(cashOrderId, OrderStatus.PREPARING);
		orderService.changeStatusAsAdmin(cashOrderId, OrderStatus.SHIPPED);
		orderService.changeStatusAsAdmin(cashOrderId, OrderStatus.DELIVERED);

		mockMvc.perform(get("/api/admin/stats").with(admin()))
			.andExpect(jsonPath("$.payments.cashToCollect").value(0))
			.andExpect(jsonPath("$.payments.collectedCash").value(25000))
			.andExpect(jsonPath("$.payments.collectedOnline").value(50000));
	}

	// Une entrée par jour, même vide : le graphique n'a pas de trou. La dernière entrée = aujourd'hui.
	@Test
	void salesReportHasOneEntryPerDayOrMonthIncludingEmptyOnes() throws Exception {
		mockMvc.perform(get("/api/admin/stats/sales").param("period", "DAY").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.points.length()").value(30))
			.andExpect(jsonPath("$.points[0].collected").value(0))
			.andExpect(jsonPath("$.points[29].start").value(LocalDate.now(AdminStatsService.SHOP_ZONE).toString()))
			.andExpect(jsonPath("$.points[29].collected").value(50000))
			.andExpect(jsonPath("$.points[29].orders").value(1));

		mockMvc.perform(get("/api/admin/stats/sales").param("period", "MONTH").with(admin()))
			.andExpect(jsonPath("$.points.length()").value(12))
			.andExpect(jsonPath("$.points[11].start")
				.value(LocalDate.now(AdminStatsService.SHOP_ZONE).withDayOfMonth(1).toString()))
			.andExpect(jsonPath("$.points[11].collected").value(50000));

		mockMvc.perform(get("/api/admin/stats/sales").param("period", "YEAR").with(admin()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void customersListShowsOrdersAndAmountSpentWithoutCancelledOrders() throws Exception {
		mockMvc.perform(get("/api/admin/customers").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2))
			// Les plus récents d'abord : Moussa (commande annulée, donc 0), puis Awa
			.andExpect(jsonPath("$.content[0].fullName").value("Moussa Fall"))
			.andExpect(jsonPath("$.content[0].orderCount").value(0))
			.andExpect(jsonPath("$.content[0].totalSpent").value(0))
			.andExpect(jsonPath("$.content[0].lastOrderAt").doesNotExist())
			.andExpect(jsonPath("$.content[1].fullName").value("Awa Diop"))
			.andExpect(jsonPath("$.content[1].orderCount").value(1))
			.andExpect(jsonPath("$.content[1].totalSpent").value(50000));

		mockMvc.perform(get("/api/admin/customers").param("q", "AWA@").with(admin()))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].email").value("awa@example.com"));
	}

	@Test
	void ordersCanBeSearchedByCustomerOrNumberAndFilteredByPaymentMethod() throws Exception {
		mockMvc.perform(get("/api/admin/orders").param("q", "diop").with(admin()))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(paidOrderId));

		mockMvc.perform(get("/api/admin/orders").param("q", "#" + paidOrderId).with(admin()))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].customerName").value("Awa Diop"));

		// « % » cherché tel quel (pas un joker qui renverrait tout)
		mockMvc.perform(get("/api/admin/orders").param("q", "%").with(admin()))
			.andExpect(jsonPath("$.totalElements").value(0));

		mockMvc.perform(get("/api/admin/orders").param("paymentMethod", "CASH_ON_DELIVERY").with(admin()))
			.andExpect(jsonPath("$.totalElements").value(0));

		// Plusieurs statuts à la fois
		mockMvc.perform(get("/api/admin/orders").param("status", "PAID,CANCELLED").with(admin()))
			.andExpect(jsonPath("$.totalElements").value(2));
		mockMvc.perform(get("/api/admin/orders").param("status", "PAID").with(admin()))
			.andExpect(jsonPath("$.totalElements").value(1));
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
