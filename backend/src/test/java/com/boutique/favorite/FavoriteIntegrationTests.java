package com.boutique.favorite;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.boutique.TestcontainersConfiguration;
import com.boutique.catalog.Category;
import com.boutique.catalog.CategoryRepository;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductRepository;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FavoriteIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FavoriteRepository favoriteRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserRepository userRepository;

	private Long boubouId;

	private RequestPostProcessor awa;

	private RequestPostProcessor moussa;

	@BeforeEach
	void setUp() {
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		boubouId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 5)).getId();
		awa = customer(userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId());
		moussa = customer(userRepository.save(new User("moussa@example.com", "Moussa Fall", Role.CUSTOMER)).getId());
	}

	@AfterEach
	void cleanUp() {
		favoriteRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void addingTwiceKeepsASingleFavoriteThenRemovingWorks() throws Exception {
		mockMvc.perform(put("/api/favorites/{id}", boubouId).with(awa)).andExpect(status().isNoContent());
		mockMvc.perform(put("/api/favorites/{id}", boubouId).with(awa)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/favorites").with(awa))
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].name").value("Boubou bazin"));
		mockMvc.perform(get("/api/favorites/ids").with(awa)).andExpect(jsonPath("$[0]").value(boubouId));

		mockMvc.perform(delete("/api/favorites/{id}", boubouId).with(awa)).andExpect(status().isNoContent());
		// Supprimer un favori absent n'est pas une erreur (idempotent)
		mockMvc.perform(delete("/api/favorites/{id}", boubouId).with(awa)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/favorites").with(awa)).andExpect(jsonPath("$.length()").value(0));
	}

	// Chaque client ne voit que SES favoris : l'identité vient du jeton
	@Test
	void favoritesArePrivate() throws Exception {
		mockMvc.perform(put("/api/favorites/{id}", boubouId).with(awa)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/favorites").with(moussa)).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void unknownProductAndAnonymousVisitorAreRefused() throws Exception {
		mockMvc.perform(put("/api/favorites/{id}", 999_999).with(awa)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/favorites")).andExpect(status().isUnauthorized());
	}

	// Un produit en favori peut toujours être supprimé du catalogue (ON DELETE CASCADE)
	@Test
	void favoriteNeverBlocksTheDeletionOfAProduct() throws Exception {
		mockMvc.perform(put("/api/favorites/{id}", boubouId).with(awa)).andExpect(status().isNoContent());

		productRepository.deleteById(boubouId);

		mockMvc.perform(get("/api/favorites").with(awa)).andExpect(jsonPath("$.length()").value(0));
	}

	private static RequestPostProcessor customer(Long id) {
		return jwt().jwt((token) -> token.issuer("boutique-api").subject(String.valueOf(id)));
	}

}
