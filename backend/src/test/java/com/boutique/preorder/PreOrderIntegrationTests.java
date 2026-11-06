package com.boutique.preorder;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.boutique.TestcontainersConfiguration;
import com.boutique.catalog.Category;
import com.boutique.catalog.CategoryRepository;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductRepository;
import com.boutique.notification.NotificationSender;
import com.boutique.order.OrderLineRequest;
import com.boutique.order.OrderRepository;
import com.boutique.order.OrderService;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PreOrderIntegrationTests {

	private static final RequestPostProcessor ADMIN = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

	@MockitoBean
	private NotificationSender notificationSender;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PreOrderRepository preOrderRepository;

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

	private Category mode;

	private Product sandales;

	private Long awaId;

	private Long moussaId;

	@BeforeEach
	void setUp() {
		mode = categoryRepository.findBySlug("mode").orElseThrow();
		sandales = productRepository.save(new Product(mode, "Sandales cuir", null, new BigDecimal("8000"), 0));
		awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		moussaId = userRepository.save(new User("moussa@example.com", "Moussa Fall", Role.CUSTOMER)).getId();
	}

	@AfterEach
	void cleanUp() {
		preOrderRepository.deleteAll();
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void availableProductCannotBePreOrdered() throws Exception {
		Long boubouId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 3)).getId();

		preOrder(awaId, boubouId, 1).andExpect(status().isConflict());
	}

	@Test
	void preOrderingTwiceKeepsASingleRequestWithTheLatestQuantity() throws Exception {
		preOrder(awaId, sandales.getId(), 1).andExpect(status().isCreated());
		preOrder(awaId, sandales.getId(), 3).andExpect(status().isCreated()).andExpect(jsonPath("$.quantity").value(3));

		mockMvc.perform(get("/api/preorders").with(customer(awaId)))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].productName").value("Sandales cuir"))
			.andExpect(jsonPath("$[0].status").value("WAITING"));
		mockMvc.perform(get("/api/admin/preorders").with(ADMIN))
			.andExpect(jsonPath("$[0].productName").value("Sandales cuir"))
			.andExpect(jsonPath("$[0].requests").value(1))
			.andExpect(jsonPath("$[0].quantity").value(3));
	}

	@Test
	void restockByAdminNotifiesWaitingCustomersOnce() throws Exception {
		preOrder(awaId, sandales.getId(), 1).andExpect(status().isCreated());
		preOrder(moussaId, sandales.getId(), 2).andExpect(status().isCreated());

		restock(5);

		verify(notificationSender).send(eq("awa@example.com"), contains("Sandales cuir"), anyString());
		verify(notificationSender).send(eq("moussa@example.com"), contains("Sandales cuir"), anyString());
		assertThat(preOrderRepository.findAll()).allMatch((p) -> p.getStatus() == PreOrder.Status.NOTIFIED);

		// 5 → 8 : ce n'est pas un retour en stock, personne n'est prévenu une deuxième fois
		restock(8);
		verify(notificationSender, times(2)).send(anyString(), anyString(), anyString());
	}

	@Test
	void stockReturnedByACancelledOrderAlsoNotifies() throws Exception {
		Product pagne = productRepository.save(new Product(mode, "Pagne wax", null, new BigDecimal("12000"), 1));
		Long moussaOrder = orderService.createOrder(moussaId, List.of(new OrderLineRequest(pagne.getId(), 1))).id();
		preOrder(awaId, pagne.getId(), 1).andExpect(status().isCreated());
		verifyNoInteractions(notificationSender);

		orderService.cancelByCustomer(moussaId, moussaOrder);

		verify(notificationSender).send(eq("awa@example.com"), contains("Pagne wax"), anyString());
	}

	@Test
	void customerCancelsOnlyHisOwnWaitingPreOrder() throws Exception {
		preOrder(awaId, sandales.getId(), 1).andExpect(status().isCreated());
		Long preOrderId = preOrderRepository.findAll().getFirst().getId();

		mockMvc.perform(delete("/api/preorders/{id}", preOrderId).with(customer(moussaId))).andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/preorders/{id}", preOrderId).with(customer(awaId))).andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/preorders/{id}", preOrderId).with(customer(awaId))).andExpect(status().isConflict());

		// Une demande annulée ne reçoit pas de notification
		restock(4);
		verifyNoInteractions(notificationSender);
	}

	private ResultActions preOrder(Long userId, Long productId, int quantity) throws Exception {
		return mockMvc.perform(post("/api/preorders").with(customer(userId))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"));
	}

	private void restock(int stock) throws Exception {
		mockMvc.perform(put("/api/products/{id}", sandales.getId()).with(ADMIN)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"categoryId\":" + mode.getId() + ",\"name\":\"Sandales cuir\",\"price\":8000,\"stock\":" + stock + "}"))
			.andExpect(status().isOk());
	}

	private static RequestPostProcessor customer(Long userId) {
		return jwt().jwt((token) -> token.issuer("boutique-api").subject(String.valueOf(userId)));
	}

}
