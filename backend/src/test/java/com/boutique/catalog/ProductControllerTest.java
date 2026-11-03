package com.boutique.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.boutique.security.SecurityConfig;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tranche web uniquement : contrôleur + GlobalExceptionHandler + nos règles de sécurité, le service est un mock.
// jwt() simule un utilisateur déjà authentifié, sans fabriquer de vrai jeton.
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

	private static final String VALID_PRODUCT = """
			{"categoryId": 1, "name": "Boubou bazin", "price": 25000, "stock": 5}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@Test
	void anyoneCanReadProducts() throws Exception {
		when(productService.findById(1L))
			.thenReturn(new ProductResponse(1L, "Boubou bazin", null, null, 5, 1L, "Mode"));

		mockMvc.perform(get("/api/products/1")).andExpect(status().isOk());
	}

	@Test
	void anonymousCannotCreateProduct() throws Exception {
		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_PRODUCT))
			.andExpect(status().isUnauthorized())
			.andExpect(header().exists("WWW-Authenticate"))
			.andExpect(jsonPath("$.title").value("Non authentifié"));

		verifyNoInteractions(productService);
	}

	@Test
	void customerCannotCreateProduct() throws Exception {
		mockMvc
			.perform(post("/api/products").with(customer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_PRODUCT))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.title").value("Accès refusé"));

		verifyNoInteractions(productService);
	}

	@Test
	void unknownProductReturns404ProblemDetail() throws Exception {
		when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

		mockMvc.perform(get("/api/products/99"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Ressource introuvable"))
			.andExpect(jsonPath("$.detail").value("Produit introuvable : 99"))
			.andExpect(jsonPath("$.instance").value("/api/products/99"));
	}

	@Test
	void invalidProductReturns400WithEveryInvalidField() throws Exception {
		mockMvc
			.perform(post("/api/products").with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
					{"name": "", "price": -5}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Données invalides"))
			.andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("categoryId", "name", "price", "stock")));

		verifyNoInteractions(productService);
	}

	@Test
	void malformedJsonReturns400() throws Exception {
		mockMvc
			.perform(post("/api/products").with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
					{"name": "Boubou", "price": "abc"}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

		verifyNoInteractions(productService);
	}

	@Test
	void unexpectedErrorReturns500WithoutTechnicalDetails() throws Exception {
		when(productService.findById(1L)).thenThrow(new IllegalStateException("connexion à db-interne:5432 refusée"));

		mockMvc.perform(get("/api/products/1"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.title").value("Erreur interne"))
			.andExpect(jsonPath("$.errorId").exists())
			.andExpect(jsonPath("$.trace").doesNotExist())
			.andExpect(content().string(not(containsString("db-interne"))));
	}

	private static RequestPostProcessor admin() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

	private static RequestPostProcessor customer() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
	}

}
