package com.boutique.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.boutique.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sans profil dev, la documentation de l'API ne doit pas être exposée (sûr par défaut)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ApiDocumentationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void openApiDocumentIsDisabledByDefault() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
	}

	@Test
	void swaggerUiIsDisabledByDefault() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
	}

}
