package com.boutique.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// En-tête de la documentation générée par springdoc (Swagger UI : /swagger-ui.html, JSON : /v3/api-docs)
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	private static final String BEARER = "bearerAuth";

	// Le bouton « Authorize » de Swagger UI permet de coller un jeton d'accès pour tester les routes protégées
	@Bean
	OpenAPI boutiqueOpenApi() {
		return new OpenAPI()
			.info(new Info().title("API Boutique")
				.version("v1")
				.description("API REST de la boutique en ligne : catalogue, commandes, paiement."))
			.components(new Components().addSecuritySchemes(BEARER,
					new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(BEARER));
	}

}
