package com.boutique.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// En-tête de la documentation générée par springdoc (Swagger UI : /swagger-ui.html, JSON : /v3/api-docs)
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	@Bean
	OpenAPI boutiqueOpenApi() {
		return new OpenAPI().info(new Info().title("API Boutique")
			.version("v1")
			.description("API REST de la boutique en ligne : catalogue, commandes, paiement."));
	}

}
