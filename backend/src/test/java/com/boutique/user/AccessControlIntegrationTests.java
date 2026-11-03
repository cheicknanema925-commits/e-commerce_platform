package com.boutique.user;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.boutique.TestcontainersConfiguration;
import com.boutique.catalog.Category;
import com.boutique.catalog.CategoryRepository;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductRepository;
import com.boutique.order.OrderLineRequest;
import com.boutique.order.OrderRepository;
import com.boutique.order.OrderService;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Qui a le droit de faire quoi : rôles CUSTOMER / ADMIN et protection IDOR, avec de vrais jetons
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AccessControlIntegrationTests {

	private static final String PASSWORD = "MotDePasse-Solide-2026";

	private static final AtomicInteger NEXT_IP = new AtomicInteger(1);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private LoginEventRepository loginEventRepository;

	private List<Long> awaOrderIds;

	@BeforeEach
	void setUp() {
		userRepository.save(new User("admin@example.com", "Admin", Role.ADMIN, passwordEncoder.encode(PASSWORD)));
		Long awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER, passwordEncoder.encode(PASSWORD))).getId();
		Long moussaId = userRepository.save(new User("moussa@example.com", "Moussa Fall", Role.CUSTOMER, passwordEncoder.encode(PASSWORD))).getId();

		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		Long boubouId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 10)).getId();
		List<OrderLineRequest> oneBoubou = List.of(new OrderLineRequest(boubouId, 1));
		awaOrderIds = List.of(orderService.createOrder(awaId, oneBoubou).id(), orderService.createOrder(awaId, oneBoubou).id());
		orderService.createOrder(moussaId, oneBoubou);
	}

	@AfterEach
	void cleanUp() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		loginEventRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void customerSeesOnlyOwnOrders() throws Exception {
		mockMvc.perform(get("/api/orders/me").header(HttpHeaders.AUTHORIZATION, bearer("awa@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].id").value(containsInAnyOrder(awaOrderIds.stream().map(Long::intValue).toArray())));

		mockMvc.perform(get("/api/orders/me").header(HttpHeaders.AUTHORIZATION, bearer("moussa@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void anonymousCannotReadOrders() throws Exception {
		mockMvc.perform(get("/api/orders/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void customerCannotReadAdminRoutes() throws Exception {
		mockMvc.perform(get("/api/admin/login-events").header(HttpHeaders.AUTHORIZATION, bearer("awa@example.com")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.title").value("Accès refusé"));
	}

	@Test
	void adminCanReadLoginAuditLog() throws Exception {
		mockMvc.perform(get("/api/admin/login-events").header(HttpHeaders.AUTHORIZATION, bearer("admin@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].email").value("admin@example.com"))
			.andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"));
	}

	@Test
	void onlyAdminCanCreateProducts() throws Exception {
		String product = """
				{"categoryId": %d, "name": "Pagne wax", "price": 12000, "stock": 30}
				""".formatted(categoryRepository.findBySlug("mode").orElseThrow().getId());

		mockMvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, bearer("awa@example.com"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(product)).andExpect(status().isForbidden());

		mockMvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, bearer("admin@example.com"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(product)).andExpect(status().isCreated());
	}

	// Connexion réelle par l'API : le jeton obtenu est exactement celui qu'aurait le frontend
	private String bearer(String email) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login").with((request) -> {
			request.setRemoteAddr("10.1.0." + NEXT_IP.getAndIncrement());
			return request;
		}).contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "%s", "password": "%s"}
				""".formatted(email, PASSWORD))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(body, "$.accessToken");
	}

}
