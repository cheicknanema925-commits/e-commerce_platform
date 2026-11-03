package com.boutique.order;

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
import org.springframework.transaction.support.TransactionTemplate;

import com.boutique.TestcontainersConfiguration;
import com.boutique.cart.CartRepository;
import com.boutique.catalog.Category;
import com.boutique.catalog.CategoryRepository;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductRepository;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Parcours complet par l'API : panier → commande → annulation ou préparation, livraison
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CheckoutIntegrationTests {

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
	private OrderService orderService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private Long awaId;

	private Long moussaId;

	private Long boubouId;

	@BeforeEach
	void setUp() {
		awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		moussaId = userRepository.save(new User("moussa@example.com", "Moussa Fall", Role.CUSTOMER)).getId();
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		boubouId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 10)).getId();
	}

	@AfterEach
	void cleanUp() {
		orderRepository.deleteAll();
		cartRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void checkoutUsesServerPricesReservesStockAndEmptiesCart() throws Exception {
		// Le client glisse un faux prix dans la requête : il est ignoré
		addToCart(awaId, """
				{"productId": %d, "quantity": 2, "price": 1}
				""".formatted(boubouId)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(50000));

		mockMvc.perform(post("/api/orders").with(customer(awaId)))
			.andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.status").value("CREATED"))
			.andExpect(jsonPath("$.totalAmount").value(50000))
			.andExpect(jsonPath("$.items[0].unitPrice").value(25000));

		mockMvc.perform(get("/api/cart").with(customer(awaId))).andExpect(jsonPath("$.items").isEmpty());
		assertThat(stockOf(boubouId)).isEqualTo(8);
	}

	@Test
	void cannotAddMoreThanAvailableStock() throws Exception {
		addToCart(awaId, """
				{"productId": %d, "quantity": 11}
				""".formatted(boubouId)).andExpect(status().isConflict());
	}

	@Test
	void checkoutWithEmptyCartIsRefused() throws Exception {
		mockMvc.perform(post("/api/orders").with(customer(awaId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Le panier est vide."));
	}

	// Le stock a baissé entre l'ajout au panier et la commande : il est revérifié, rien n'est enregistré
	@Test
	void stockIsCheckedAgainAtCheckout() throws Exception {
		addToCart(awaId, """
				{"productId": %d, "quantity": 5}
				""".formatted(boubouId)).andExpect(status().isOk());
		transactionTemplate.executeWithoutResult((status) -> productRepository.findById(boubouId).orElseThrow().removeStock(7));

		mockMvc.perform(post("/api/orders").with(customer(awaId))).andExpect(status().isConflict());

		assertThat(orderRepository.count()).isZero();
		assertThat(stockOf(boubouId)).isEqualTo(3);
		mockMvc.perform(get("/api/cart").with(customer(awaId))).andExpect(jsonPath("$.items.length()").value(1));
	}

	// IDOR avec un id dans l'URL : Moussa ne peut ni lire ni annuler la commande d'Awa (404, pas même un 403)
	@Test
	void customerCannotReadOrCancelSomeoneElsesOrder() throws Exception {
		Long awaOrderId = placeOrder(awaId);

		mockMvc.perform(get("/api/orders/{id}", awaOrderId).with(customer(moussaId))).andExpect(status().isNotFound());
		mockMvc.perform(post("/api/orders/{id}/cancel", awaOrderId).with(customer(moussaId)))
			.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/orders/{id}", awaOrderId).with(customer(awaId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CREATED"));
	}

	@Test
	void customerCancelsCreatedOrderAndStockIsRestored() throws Exception {
		Long orderId = placeOrder(awaId);
		assertThat(stockOf(boubouId)).isEqualTo(9);

		mockMvc.perform(post("/api/orders/{id}/cancel", orderId).with(customer(awaId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));

		assertThat(stockOf(boubouId)).isEqualTo(10);
	}

	@Test
	void customerCannotCancelPaidOrder() throws Exception {
		Long orderId = placeOrder(awaId);
		orderService.markPaid(orderId);

		mockMvc.perform(post("/api/orders/{id}/cancel", orderId).with(customer(awaId)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Changement de statut impossible : PAID → CANCELLED."));
	}

	@Test
	void adminMovesOrderThroughTheStateMachine() throws Exception {
		Long orderId = placeOrder(awaId);
		orderService.markPaid(orderId);

		changeStatus(orderId, "PREPARING").andExpect(status().isOk());
		// On ne saute pas l'expédition
		changeStatus(orderId, "DELIVERED").andExpect(status().isConflict());
		changeStatus(orderId, "SHIPPED").andExpect(status().isOk());
		changeStatus(orderId, "DELIVERED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"));

		mockMvc.perform(get("/api/admin/orders").param("status", "DELIVERED").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(orderId));
	}

	@Test
	void customerCannotUseAdminOrderRoutes() throws Exception {
		Long orderId = placeOrder(awaId);

		mockMvc.perform(patch("/api/admin/orders/{id}/status", orderId).with(customer(awaId))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"DELIVERED\"}")).andExpect(status().isForbidden());
	}

	private Long placeOrder(Long userId) throws Exception {
		addToCart(userId, """
				{"productId": %d, "quantity": 1}
				""".formatted(boubouId)).andExpect(status().isOk());
		String body = mockMvc.perform(post("/api/orders").with(customer(userId)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private ResultActions addToCart(Long userId, String body) throws Exception {
		return mockMvc.perform(post("/api/cart/items").with(customer(userId))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private ResultActions changeStatus(Long orderId, String status) throws Exception {
		return mockMvc.perform(patch("/api/admin/orders/{id}/status", orderId).with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\": \"%s\"}".formatted(status)));
	}

	private int stockOf(Long productId) {
		return productRepository.findById(productId).orElseThrow().getStock();
	}

	// Jeton simulé de NOTRE émetteur : « sub » = id local de l'utilisateur
	private static RequestPostProcessor customer(Long userId) {
		return jwt().jwt((token) -> token.issuer("boutique-api").subject(userId.toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
	}

	private static RequestPostProcessor admin() {
		return jwt().jwt((token) -> token.issuer("boutique-api").subject("0"))
			.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

}
