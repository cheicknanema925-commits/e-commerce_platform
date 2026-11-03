package com.boutique.user;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.boutique.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Comptes Keycloak : jetons simulés avec jwt() (pas de serveur Keycloak dans la CI).
// La conversion des rôles est testée à part (KeycloakRealmRolesConverterTest) et le parcours réel à la main.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class KeycloakAccountIntegrationTests {

	private static final String KEYCLOAK_ISSUER = "http://keycloak.test/realms/boutique";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtEncoder jwtEncoder;

	@AfterEach
	void cleanUp() {
		userRepository.deleteAll();
	}

	@Test
	void firstKeycloakRequestCreatesALinkedLocalAccount() throws Exception {
		String subject = UUID.randomUUID().toString();

		mockMvc.perform(get("/api/users/me").with(keycloakUser(subject, "Fatou@Example.com", true, "CUSTOMER")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("fatou@example.com"))
			.andExpect(jsonPath("$.fullName").value("Fatou Sow"))
			.andExpect(jsonPath("$.role").value("CUSTOMER"));

		// Deuxième visite : le même compte local est retrouvé grâce au « sub » Keycloak, aucun doublon
		mockMvc.perform(get("/api/users/me").with(keycloakUser(subject, "fatou@example.com", true, "CUSTOMER")))
			.andExpect(status().isOk());
		assertThat(userRepository.findAll()).hasSize(1)
			.first()
			.extracting(User::getExternalId)
			.isEqualTo(subject);
	}

	@Test
	void keycloakAdminIsProvisionedAsAdmin() throws Exception {
		mockMvc.perform(get("/api/users/me").with(keycloakUser(UUID.randomUUID().toString(), "mariama@example.com", true, "ADMIN")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void verifiedKeycloakEmailIsLinkedToExistingLocalAccount() throws Exception {
		Long localId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		String subject = UUID.randomUUID().toString();

		mockMvc.perform(get("/api/users/me").with(keycloakUser(subject, "awa@example.com", true, "CUSTOMER")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(localId));

		assertThat(userRepository.findById(localId).orElseThrow().getExternalId()).isEqualTo(subject);
	}

	// Sans email vérifié, quelqu'un pourrait créer un compte Keycloak avec l'email d'un autre et prendre son compte
	@Test
	void unverifiedEmailCannotTakeOverLocalAccount() throws Exception {
		Long localId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();

		mockMvc.perform(get("/api/users/me").with(keycloakUser(UUID.randomUUID().toString(), "awa@example.com", false, "CUSTOMER")))
			.andExpect(status().isConflict());

		assertThat(userRepository.findById(localId).orElseThrow().getExternalId()).isNull();
	}

	// Un jeton correctement signé mais venant d'un émetteur inconnu est refusé
	@Test
	void tokenFromUnknownIssuerIsRejected() throws Exception {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("http://pirate.example/realms/boutique")
			.subject("1")
			.issuedAt(now)
			.expiresAt(now.plus(Duration.ofMinutes(5)))
			.claim("roles", List.of("ADMIN"))
			.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();

		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	private static RequestPostProcessor keycloakUser(String subject, String email, boolean emailVerified, String role) {
		return jwt().jwt((token) -> token.issuer(KEYCLOAK_ISSUER)
			.subject(subject)
			.claim("email", email)
			.claim("email_verified", emailVerified)
			.claim("name", email.startsWith("mariama") ? "Mariama Ba" : "Fatou Sow")
			.claim("realm_access", Map.of("roles", List.of(role))))
			.authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

}
