package com.boutique.catalog;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.boutique.TestcontainersConfiguration;
import com.boutique.order.OrderLineRequest;
import com.boutique.order.OrderRepository;
import com.boutique.order.OrderService;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Application complète + vraie base PostgreSQL : pour les erreurs que seule la base peut produire
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@AfterEach
	void cleanUp() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void sortingOnUnknownPropertyReturns400() throws Exception {
		mockMvc.perform(get("/api/products").param("sort", "motdepasse"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Propriété inconnue : motdepasse"));
	}

	@Test
	void deletingAnOrderedProductReturns409AndKeepsIt() throws Exception {
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		Long productId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 5))
			.getId();
		Long userId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		orderService.createOrder(userId, List.of(new OrderLineRequest(productId, 1)));

		mockMvc.perform(delete("/api/products/{id}", productId))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Opération impossible : cette ressource est liée à d'autres données."));

		assertThat(productRepository.existsById(productId)).isTrue();
	}

	@Test
	void unknownUrlReturns404ProblemDetail() throws Exception {
		mockMvc.perform(get("/api/nexiste-pas"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
	}

}
