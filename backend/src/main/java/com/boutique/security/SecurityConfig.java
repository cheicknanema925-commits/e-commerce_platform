package com.boutique.security;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProperties properties, JwtDecoder jwtDecoder,
			JsonMapper jsonMapper) throws Exception {
		SecurityProblemHandler problemHandler = new SecurityProblemHandler(jsonMapper);
		http
			// Pas de session côté serveur : chaque requête porte son jeton
			.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// CSRF inutile ici : le jeton voyage dans l'en-tête Authorization, que le navigateur n'envoie jamais tout seul
			.csrf((csrf) -> csrf.disable())
			// Règles lues dans l'ordre : la première qui correspond s'applique
			.authorizeHttpRequests((auth) -> auth.requestMatchers("/api/auth/**")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/images/**", "/api/store/settings")
				.permitAll()
				// Appelé par le fournisseur de paiement, sans jeton : l'authenticité est vérifiée dans PaymentService
				.requestMatchers(HttpMethod.POST, "/api/newsletter/subscriptions", "/api/newsletter/unsubscribe")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/payments/webhooks/**")
				.permitAll()
				.requestMatchers("/actuator/health/**", "/error")
				.permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
				.permitAll()
				.requestMatchers("/api/admin/**")
				.hasRole("ADMIN")
				.anyRequest()
				.authenticated())
			// L'API est un « resource server » : elle vérifie les JWT de deux émetteurs de confiance (elle-même et Keycloak)
			.oauth2ResourceServer((oauth2) -> oauth2
				.authenticationManagerResolver(authenticationManagerResolver(properties, jwtDecoder))
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

	// Vérifie NOS jetons : signature HS256 (clé partagée), expiration et émetteur
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

	// Lit le claim « iss » du jeton et choisit le bon vérificateur. Émetteur inconnu = jeton refusé (401).
	private static JwtIssuerAuthenticationManagerResolver authenticationManagerResolver(SecurityProperties properties,
			JwtDecoder localDecoder) {
		Map<String, AuthenticationManager> managers = new HashMap<>();
		managers.put(properties.jwt().issuer(), jwtAuthenticationManager(localDecoder, localRolesConverter()));
		if (properties.keycloak().enabled()) {
			managers.put(properties.keycloak().issuerUri(),
					jwtAuthenticationManager(keycloakDecoder(properties.keycloak()), new KeycloakRealmRolesConverter()));
		}
		return new JwtIssuerAuthenticationManagerResolver(managers::get);
	}

	// Vérifie les jetons KEYCLOAK : signature RS256 avec ses clés publiques (JWKS), émetteur ET audience.
	// Les clés sont téléchargées au premier jeton reçu : l'API démarre même si Keycloak est arrêté.
	private static JwtDecoder keycloakDecoder(SecurityProperties.Keycloak keycloak) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder
			.withJwkSetUri(keycloak.issuerUri() + "/protocol/openid-connect/certs")
			.build();
		// L'audience prouve que le jeton a été émis POUR cette API, pas pour une autre application du même realm
		JwtClaimValidator<Collection<String>> audience = new JwtClaimValidator<>(JwtClaimNames.AUD,
				(aud) -> aud != null && aud.contains(keycloak.audience()));
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(keycloak.issuerUri()), audience));
		return decoder;
	}

	private static AuthenticationManager jwtAuthenticationManager(JwtDecoder decoder,
			Converter<Jwt, Collection<GrantedAuthority>> rolesConverter) {
		JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(rolesConverter);
		JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
		provider.setJwtAuthenticationConverter(authenticationConverter);
		return provider::authenticate;
	}

	// Nos jetons : le claim « roles » (ex. ["ADMIN"]) devient l'autorisation Spring ROLE_ADMIN → hasRole("ADMIN")
	private static Converter<Jwt, Collection<GrantedAuthority>> localRolesConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("roles");
		authorities.setAuthorityPrefix("ROLE_");
		return authorities;
	}

	private static SecretKey secretKey(SecurityProperties properties) {
		return new SecretKeySpec(properties.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

}
