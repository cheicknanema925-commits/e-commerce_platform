package com.boutique.support;

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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SupportTicketIntegrationTests {

	private static final RequestPostProcessor ADMIN = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

	@MockitoBean
	private NotificationSender notificationSender;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SupportTicketRepository ticketRepository;

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

	private Long awaOrderId;

	@BeforeEach
	void setUp() {
		awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		moussaId = userRepository.save(new User("moussa@example.com", "Moussa Fall", Role.CUSTOMER)).getId();
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		Long boubouId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 10)).getId();
		awaOrderId = orderService.createOrder(awaId, List.of(new OrderLineRequest(boubouId, 1))).id();
	}

	@AfterEach
	void cleanUp() {
		ticketRepository.deleteAll();
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void customerOpensATicketAboutHisOrderThenSeesIt() throws Exception {
		open(customer(awaId), "DELIVERY", "Colis en retard", awaOrderId).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andExpect(jsonPath("$.orderId").value(awaOrderId));

		mockMvc.perform(get("/api/support/tickets").with(customer(awaId)))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].subject").value("Colis en retard"));
		mockMvc.perform(get("/api/support/tickets").with(customer(moussaId))).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void ticketAboutSomeoneElsesOrderIsNotFound() throws Exception {
		// IDOR : Moussa ne peut pas rattacher sa demande à la commande d'Awa (ni apprendre qu'elle existe)
		open(customer(moussaId), "ORDER", "Ma commande", awaOrderId).andExpect(status().isNotFound());
	}

	@Test
	void invalidTicketIsRejected() throws Exception {
		open(customer(awaId), "DELIVERY", " ", null).andExpect(status().isBadRequest());
		open(customer(awaId), "INCONNU", "Objet", null).andExpect(status().isBadRequest());
	}

	@Test
	void openTicketsAreLimited() throws Exception {
		for (int i = 0; i < SupportTicketService.MAX_OPEN_TICKETS; i++) {
			open(customer(awaId), "OTHER", "Question " + i, null).andExpect(status().isCreated());
		}

		open(customer(awaId), "OTHER", "Une de trop", null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("5 demandes")));
	}

	@Test
	void adminRepliesAndCustomerIsNotified() throws Exception {
		Long ticketId = openAndGetId();

		mockMvc.perform(get("/api/admin/support/tickets").param("status", "OPEN").with(ADMIN))
			.andExpect(jsonPath("$.content[0].customerName").value("Awa Diop"))
			.andExpect(jsonPath("$.content[0].ticket.subject").value("Colis en retard"));
		reply(ticketId, "Votre colis part demain.").andExpect(status().isOk())
			.andExpect(jsonPath("$.ticket.status").value("ANSWERED"));

		verify(notificationSender).send(eq("awa@example.com"), contains("Colis en retard"), anyString());
		mockMvc.perform(get("/api/support/tickets").with(customer(awaId)))
			.andExpect(jsonPath("$[0].reply").value("Votre colis part demain."));
	}

	@Test
	void closedTicketCannotBeAnswered() throws Exception {
		Long ticketId = openAndGetId();
		mockMvc.perform(put("/api/admin/support/tickets/{id}/close", ticketId).with(ADMIN)).andExpect(status().isOk());

		reply(ticketId, "Trop tard").andExpect(status().isConflict());
	}

	@Test
	void customerCannotUseAdminRoutes() throws Exception {
		Long ticketId = openAndGetId();

		mockMvc.perform(get("/api/admin/support/tickets").with(customer(awaId))).andExpect(status().isForbidden());
		mockMvc.perform(put("/api/admin/support/tickets/{id}/reply", ticketId).with(customer(awaId))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"reply\":\"Je me réponds\"}")).andExpect(status().isForbidden());
	}

	private Long openAndGetId() throws Exception {
		open(customer(awaId), "DELIVERY", "Colis en retard", awaOrderId).andExpect(status().isCreated());
		return ticketRepository.findAll().getFirst().getId();
	}

	private ResultActions open(RequestPostProcessor user, String category, String subject, Long orderId) throws Exception {
		return mockMvc.perform(post("/api/support/tickets").with(user)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"category\":\"" + category + "\",\"subject\":\"" + subject
					+ "\",\"message\":\"Je n'ai toujours rien reçu.\",\"orderId\":" + orderId + "}"));
	}

	private ResultActions reply(Long ticketId, String reply) throws Exception {
		return mockMvc.perform(put("/api/admin/support/tickets/{id}/reply", ticketId).with(ADMIN)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"reply\":\"" + reply + "\"}"));
	}

	private static RequestPostProcessor customer(Long userId) {
		return jwt().jwt((token) -> token.issuer("boutique-api").subject(String.valueOf(userId)));
	}

}
