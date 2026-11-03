package com.boutique.user;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.boutique.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Parcours complets d'authentification avec de vrais jetons, une vraie base (Testcontainers) et l'API HTTP
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTests {

	private static final String PASSWORD = "MotDePasse-Solide-2026";

	// Chaque test utilise sa propre adresse IP pour ne pas déclencher la limitation de débit des autres
	private static final AtomicInteger NEXT_IP = new AtomicInteger(1);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private LoginEventRepository loginEventRepository;

	@Autowired
	private JwtEncoder jwtEncoder;

	@AfterEach
	void cleanUp() {
		loginEventRepository.deleteAll();
		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void registerAlwaysCreatesCustomerEvenIfAdminRoleIsSent() throws Exception {
		mockMvc.perform(post("/api/auth/register").with(fromIp(nextIp())).contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "Awa@Example.com", "fullName": "Awa Diop", "password": "%s", "role": "ADMIN"}
				""".formatted(PASSWORD)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("awa@example.com"))
			.andExpect(jsonPath("$.role").value("CUSTOMER"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());

		assertThat(userRepository.findByEmail("awa@example.com").orElseThrow().getPasswordHash()).startsWith("{bcrypt}");
	}

	@Test
	void registerRejectsDuplicateEmail() throws Exception {
		register("awa@example.com");

		mockMvc.perform(post("/api/auth/register").with(fromIp(nextIp())).contentType(MediaType.APPLICATION_JSON).content(registerBody("awa@example.com")))
			.andExpect(status().isConflict());
	}

	@Test
	void registerRejectsPasswordLongerThan72Bytes() throws Exception {
		// 50 « é » = 50 caractères (accepté par @Size) mais 100 octets en UTF-8 : refusé proprement, pas d'erreur 500
		String password = "é".repeat(50);

		mockMvc.perform(post("/api/auth/register").with(fromIp(nextIp())).contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "awa@example.com", "fullName": "Awa Diop", "password": "%s"}
				""".formatted(password)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Le mot de passe est trop long (72 octets maximum)."));
	}

	@Test
	void loginReturnsTokensThatOpenTheProfile() throws Exception {
		register("awa@example.com");
		String body = login("awa@example.com", PASSWORD, nextIp()).andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900))
			.andReturn()
			.getResponse()
			.getContentAsString();

		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + JsonPath.read(body, "$.accessToken")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("awa@example.com"))
			.andExpect(jsonPath("$.role").value("CUSTOMER"));
	}

	@Test
	void profileWithoutTokenReturns401ProblemDetail() throws Exception {
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isUnauthorized())
			// Spring Security 7 ajoute aussi resource_metadata (RFC 9728) : on vérifie seulement le schéma Bearer
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Non authentifié"));
	}

	@Test
	void expiredTokenIsRejected() throws Exception {
		Long userId = register("awa@example.com");
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("boutique-api")
			.subject(userId.toString())
			.issuedAt(now.minus(Duration.ofHours(1)))
			.expiresAt(now.minus(Duration.ofMinutes(30)))
			.claim("roles", List.of("CUSTOMER"))
			.build();
		String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();

		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tamperedTokenIsRejected() throws Exception {
		register("awa@example.com");
		String body = login("awa@example.com", PASSWORD, nextIp()).andReturn().getResponse().getContentAsString();
		String[] parts = JsonPath.<String>read(body, "$.accessToken").split("\\.");

		// L'attaquant se donne le rôle ADMIN dans le contenu du jeton… mais garde l'ancienne signature
		String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8).replace("CUSTOMER", "ADMIN");
		String forged = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

		mockMvc.perform(get("/api/admin/login-events").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void unknownEmailAndWrongPasswordGiveTheSameAnswer() throws Exception {
		register("awa@example.com");
		String ip = nextIp();

		String wrongPassword = login("awa@example.com", "mauvais-mot-de-passe", ip).andExpect(status().isUnauthorized())
			.andReturn().getResponse().getContentAsString();
		String unknownEmail = login("inconnu@example.com", PASSWORD, ip).andExpect(status().isUnauthorized())
			.andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(unknownEmail, "$.detail")).isEqualTo(JsonPath.read(wrongPassword, "$.detail"));
	}

	@Test
	void accountIsLockedAfterFiveFailedAttempts() throws Exception {
		register("awa@example.com");
		String ip = nextIp();
		for (int i = 0; i < 5; i++) {
			login("awa@example.com", "mauvais-mot-de-passe", ip).andExpect(status().isUnauthorized());
		}

		// Le bon mot de passe ne suffit plus : le compte est verrouillé
		login("awa@example.com", PASSWORD, ip).andExpect(status().isUnauthorized());

		assertThat(loginEventRepository.findByEmailOrderByIdAsc("awa@example.com")).extracting(LoginEvent::getOutcome)
			.containsExactly(LoginOutcome.BAD_CREDENTIALS, LoginOutcome.BAD_CREDENTIALS, LoginOutcome.BAD_CREDENTIALS,
					LoginOutcome.BAD_CREDENTIALS, LoginOutcome.BAD_CREDENTIALS, LoginOutcome.LOCKED);
	}

	@Test
	void refreshTokenIsRotatedAndReuseRevokesEverything() throws Exception {
		register("awa@example.com");
		String firstRefresh = JsonPath.read(login("awa@example.com", PASSWORD, nextIp()).andReturn().getResponse().getContentAsString(), "$.refreshToken");

		String secondRefresh = JsonPath.read(refresh(firstRefresh).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.refreshToken");
		assertThat(secondRefresh).isNotEqualTo(firstRefresh);

		// Le premier jeton revient alors qu'il a déjà servi : vol suspecté → refus, et TOUTES les sessions sont coupées
		refresh(firstRefresh).andExpect(status().isUnauthorized());
		refresh(secondRefresh).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesRefreshToken() throws Exception {
		register("awa@example.com");
		String refreshToken = JsonPath.read(login("awa@example.com", PASSWORD, nextIp()).andReturn().getResponse().getContentAsString(), "$.refreshToken");

		mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refreshToken)))
			.andExpect(status().isNoContent());

		refresh(refreshToken).andExpect(status().isUnauthorized());
	}

	@Test
	void loginIsRateLimitedPerIpAddress() throws Exception {
		String ip = nextIp();
		for (int i = 0; i < 10; i++) {
			login("inconnu@example.com", PASSWORD, ip).andExpect(status().isUnauthorized());
		}

		login("inconnu@example.com", PASSWORD, ip).andExpect(status().isTooManyRequests())
			.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
			.andExpect(jsonPath("$.title").value("Trop de tentatives"));

		// Une autre adresse IP n'est pas bloquée
		login("inconnu@example.com", PASSWORD, nextIp()).andExpect(status().isUnauthorized());
	}

	// Audit OWASP : un jeton « alg: none » (sans signature) ne doit jamais être accepté
	@Test
	void unsignedAlgNoneTokenIsRejected() throws Exception {
		Base64.Encoder base64 = Base64.getUrlEncoder().withoutPadding();
		String header = base64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
		String payload = base64.encodeToString("{\"iss\":\"boutique-api\",\"sub\":\"1\",\"roles\":[\"ADMIN\"],\"exp\":4102444800}"
			.getBytes(StandardCharsets.UTF_8));

		mockMvc.perform(get("/api/admin/login-events").header(HttpHeaders.AUTHORIZATION, "Bearer " + header + "." + payload + "."))
			.andExpect(status().isUnauthorized());
	}

	// Audit OWASP : un email géant faisait échouer l'écriture du journal d'audit (réponse 409 et tentative non tracée)
	@Test
	void oversizedLoginInputIsRejectedBeforeReachingTheDatabase() throws Exception {
		login("a".repeat(300) + "@example.com", PASSWORD, nextIp()).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("email"));
	}

	// Audit OWASP : sans limite, on pouvait créer des comptes en masse et énumérer les emails existants
	@Test
	void registerIsRateLimitedPerIpAddress() throws Exception {
		String ip = nextIp();
		for (int i = 0; i < 10; i++) {
			mockMvc.perform(post("/api/auth/register").with(fromIp(ip)).contentType(MediaType.APPLICATION_JSON)
				.content(registerBody("client" + i + "@example.com"))).andExpect(status().isCreated());
		}

		mockMvc.perform(post("/api/auth/register").with(fromIp(ip)).contentType(MediaType.APPLICATION_JSON)
			.content(registerBody("client10@example.com"))).andExpect(status().isTooManyRequests());
	}

	// Audit OWASP : en-têtes de sécurité ajoutés par Spring Security sur chaque réponse
	@Test
	void responsesCarrySecurityHeaders() throws Exception {
		mockMvc.perform(get("/api/products"))
			.andExpect(header().string("X-Content-Type-Options", "nosniff"))
			.andExpect(header().string("X-Frame-Options", "DENY"))
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
	}

	private Long register(String email) throws Exception {
		String body = mockMvc.perform(post("/api/auth/register").with(fromIp(nextIp())).contentType(MediaType.APPLICATION_JSON).content(registerBody(email)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	private ResultActions login(String email, String password, String ip) throws Exception {
		return mockMvc.perform(post("/api/auth/login").with(fromIp(ip)).contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "%s", "password": "%s"}
				""".formatted(email, password)));
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refreshToken)));
	}

	private static String registerBody(String email) {
		return """
				{"email": "%s", "fullName": "Awa Diop", "password": "%s"}
				""".formatted(email, PASSWORD);
	}

	private static String refreshBody(String refreshToken) {
		return """
				{"refreshToken": "%s"}
				""".formatted(refreshToken);
	}

	private static String nextIp() {
		return "10.0.0." + NEXT_IP.getAndIncrement();
	}

	private static RequestPostProcessor fromIp(String ip) {
		return (request) -> {
			request.setRemoteAddr(ip);
			return request;
		};
	}

}
