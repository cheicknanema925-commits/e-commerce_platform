package com.boutique.review;

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
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Avis : seulement après avoir acheté ET reçu le produit, une seule fois, et sans exposer l'identité de l'auteur
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ReviewIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private OrderService orderService;

	private Long awaId;

	private Long moussaId;

	private Long boubouId;

	// Awa a acheté et REÇU le boubou ; Moussa ne l'a jamais acheté
	@BeforeEach
	void setUp() {
		awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		moussaId = userRepository.save(new User("moussa@example.com", "Moussa Fall", Role.CUSTOMER)).getId();
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		boubouId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 10)).getId();

		Long orderId = orderService.createOrder(awaId, List.of(new OrderLineRequest(boubouId, 1))).id();
		orderService.markPaid(orderId);
		orderService.changeStatusAsAdmin(orderId, OrderStatus.PREPARING);
		orderService.changeStatusAsAdmin(orderId, OrderStatus.SHIPPED);
		orderService.changeStatusAsAdmin(orderId, OrderStatus.DELIVERED);
	}

	@AfterEach
	void cleanUp() {
		reviewRepository.deleteAll();
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void customerReviewsAReceivedProductOnlyOnce() throws Exception {
		review(awaId, 5, "Très beau bazin, livré à temps.").andExpect(status().isCreated())
			.andExpect(jsonPath("$.author").value("Awa D."));

		review(awaId, 1, "Je change d'avis").andExpect(status().isConflict());

		// Lecture publique, sans jeton : prénom + initiale, jamais l'email
		mockMvc.perform(get("/api/products/{id}/reviews", boubouId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].author").value("Awa D."))
			.andExpect(jsonPath("$.content[0].email").doesNotExist());
		mockMvc.perform(get("/api/products/{id}/rating", boubouId))
			.andExpect(jsonPath("$.average").value(5.0))
			.andExpect(jsonPath("$.count").value(1));
	}

	@Test
	void customerWhoNeverReceivedTheProductCannotReview() throws Exception {
		review(moussaId, 1, "Nul (je ne l'ai jamais acheté)").andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value("Vous ne pouvez noter qu'un produit que vous avez acheté et reçu."));
	}

	@Test
	void anonymousCannotReview() throws Exception {
		mockMvc.perform(post("/api/products/{id}/reviews", boubouId).contentType(MediaType.APPLICATION_JSON)
			.content("{\"rating\": 5}")).andExpect(status().isUnauthorized());
	}

	@Test
	void ratingMustBeBetweenOneAndFive() throws Exception {
		review(awaId, 6, "Plus que parfait").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("rating"));
	}

	@Test
	void productWithoutReviewsHasZeroRating() throws Exception {
		mockMvc.perform(get("/api/products/{id}/rating", boubouId))
			.andExpect(jsonPath("$.average").value(0.0))
			.andExpect(jsonPath("$.count").value(0));
	}

	private ResultActions review(Long userId, int rating, String comment) throws Exception {
		return mockMvc.perform(post("/api/products/{id}/reviews", boubouId).with(customer(userId))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"rating": %d, "comment": "%s"}
					""".formatted(rating, comment)));
	}

	private static RequestPostProcessor customer(Long userId) {
		return jwt().jwt((token) -> token.issuer("boutique-api").subject(userId.toString()))
			.authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
	}

}
