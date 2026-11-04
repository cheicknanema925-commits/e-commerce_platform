package com.boutique.order;

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
import com.boutique.payment.PaymentRepository;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CourierIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private DeliveryRepository deliveryRepository;

	@Autowired
	private CourierRepository courierRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserRepository userRepository;

	private Long orderId;

	private RequestPostProcessor awa;

	// Commande en espèces d'Awa, avec sa livraison
	@BeforeEach
	void setUp() {
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		Long productId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 5)).getId();
		Long awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		awa = jwt().jwt((token) -> token.issuer("boutique-api").subject(String.valueOf(awaId)));
		orderId = orderService.createOrder(awaId, List.of(new OrderLineRequest(productId, 1)), PaymentMethod.CASH_ON_DELIVERY).id();
		deliveryRepository.save(new Delivery(orderRepository.findById(orderId).orElseThrow(), "Rue 10", "Dakar", "+221771234567", null, null));
	}

	@AfterEach
	void cleanUp() {
		paymentRepository.deleteAll();
		deliveryRepository.deleteAll();
		courierRepository.deleteAll();
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void courierIsShownToTheCustomerOnlyOnceTheParcelHasLeft() throws Exception {
		Long abdouId = createCourier("Abdou K.");
		assign(abdouId).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/admin/orders/{id}", orderId).with(admin())).andExpect(jsonPath("$.courierName").value("Abdou K."));
		mockMvc.perform(get("/api/admin/couriers").with(admin())).andExpect(jsonPath("$[0].ongoingDeliveries").value(1));
		// Pas encore partie : le client ne voit pas encore le livreur (l'affectation peut changer)
		mockMvc.perform(get("/api/orders/{id}/delivery", orderId).with(awa)).andExpect(jsonPath("$.courierName").doesNotExist());

		orderService.changeStatusAsAdmin(orderId, OrderStatus.PREPARING);
		orderService.changeStatusAsAdmin(orderId, OrderStatus.SHIPPED);

		mockMvc.perform(get("/api/orders/{id}/delivery", orderId).with(awa))
			.andExpect(jsonPath("$.courierName").value("Abdou K."))
			.andExpect(jsonPath("$.courierPhone").value("+221 77 000 00 01"));
	}

	@Test
	void deactivatedCourierCannotReceiveAnOrder() throws Exception {
		Long abdouId = createCourier("Abdou K.");
		mockMvc.perform(put("/api/admin/couriers/{id}", abdouId).with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"Abdou K.\", \"phone\": \"+221 77 000 00 01\", \"active\": false}"))
			.andExpect(jsonPath("$.active").value(false));

		assign(abdouId).andExpect(status().isConflict());
	}

	@Test
	void finishedOrderKeepsItsCourier() throws Exception {
		Long abdouId = createCourier("Abdou K.");
		orderService.changeStatusAsAdmin(orderId, OrderStatus.CANCELLED);

		assign(abdouId).andExpect(status().isConflict());
	}

	@Test
	void customerCannotManageCouriers() throws Exception {
		mockMvc.perform(get("/api/admin/couriers").with(awa)).andExpect(status().isForbidden());
	}

	private Long createCourier(String name) throws Exception {
		String body = mockMvc.perform(post("/api/admin/couriers").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\": \"%s\", \"phone\": \"+221 77 000 00 01\", \"active\": true}".formatted(name)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private ResultActions assign(Long courierId) throws Exception {
		return mockMvc.perform(put("/api/admin/orders/{id}/courier", orderId).with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"courierId\": %d}".formatted(courierId)));
	}

	private static RequestPostProcessor admin() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

}
