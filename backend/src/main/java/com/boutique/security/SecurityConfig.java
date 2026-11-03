package com.boutique.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProperties properties, JsonMapper jsonMapper)
			throws Exception {
		SecurityProblemHandler problemHandler = new SecurityProblemHandler(jsonMapper);
		http
			// Pas de session côté serveur : chaque requête porte son jeton
			.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// CSRF inutile ici : le jeton voyage dans l'en-tête Authorization, que le navigateur n'envoie jamais tout seul
			.csrf((csrf) -> csrf.disable())
			// Règles lues dans l'ordre : la première qui correspond s'applique
			.authorizeHttpRequests((auth) -> auth.requestMatchers("/api/auth/**")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**")
				.permitAll()
				.requestMatchers("/actuator/health/**", "/error")
				.permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
				.permitAll()
				.requestMatchers("/api/admin/**")
				.hasRole("ADMIN")
				.anyRequest()
				.authenticated())
			// L'API est un « resource server » : elle vérifie les JWT (signature, expiration, émetteur)
			.oauth2ResourceServer((oauth2) -> oauth2.jwt((jwt) -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
				.authenticationEntryPoint(problemHandler)
				.accessDeniedHandler(problemHandler))
			.exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint(problemHandler)
				.accessDeniedHandler(problemHandler))
			.addFilterBefore(new AuthRateLimitFilter(properties.rateLimit().requestsPerMinute(), problemHandler),
					BearerTokenAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	JwtEncoder jwtEncoder(SecurityProperties properties) {
		return NimbusJwtEncoder.withSecretKey(secretKey(properties)).algorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecurityProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()));
		return decoder;
	}

	// BCrypt par défaut ; le hash commence par {bcrypt}, ce qui permettra de changer d'algorithme plus tard
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	// Le claim « roles » du jeton (ex. ["ADMIN"]) devient l'autorisation Spring ROLE_ADMIN → hasRole("ADMIN")
	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("roles");
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	private static SecretKey secretKey(SecurityProperties properties) {
		return new SecretKeySpec(properties.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
