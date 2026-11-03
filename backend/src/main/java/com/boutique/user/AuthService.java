package com.boutique.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.common.PageResponse;
import com.boutique.security.SecurityProperties;

@Service
@Transactional
public class AuthService {

	private static final int BCRYPT_MAX_BYTES = 72;

	private final UserRepository userRepository;

	private final RefreshTokenRepository refreshTokenRepository;

	private final LoginEventRepository loginEventRepository;

	private final PasswordEncoder passwordEncoder;

	private final JwtEncoder jwtEncoder;

	private final SecurityProperties properties;

	private final SecureRandom secureRandom = new SecureRandom();

	// Hash factice : un email inconnu coûte le même temps de calcul qu'un vrai compte (pas d'indice par la durée)
	private final String dummyPasswordHash;

	public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
			LoginEventRepository loginEventRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
			SecurityProperties properties) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.loginEventRepository = loginEventRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.dummyPasswordHash = passwordEncoder.encode("mot-de-passe-factice-pour-egaliser-les-temps");
	}

	public UserResponse register(RegisterRequest request) {
		if (tooLongForBcrypt(request.password())) {
			throw new PasswordTooLongException();
		}
		String email = normalize(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyUsedException();
		}
		User user = new User(email, request.fullName().strip(), Role.CUSTOMER,
				passwordEncoder.encode(request.password()));
		return UserResponse.from(userRepository.save(user));
	}

	// noRollbackFor : le compteur d'échecs et le journal d'audit doivent être enregistrés MÊME quand la
	// connexion échoue. Sans lui, l'exception annulerait la transaction... et le verrouillage ne marcherait jamais.
	@Transactional(noRollbackFor = InvalidCredentialsException.class)
	public IssuedTokens login(LoginRequest request, String ipAddress) {
		String email = normalize(request.email());
		Instant now = Instant.now();
		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null) {
			passwordMatches(request.password(), null);
			throw failure(email, null, ipAddress, LoginOutcome.UNKNOWN_EMAIL);
		}
		if (user.isLocked(now)) {
			throw failure(email, user.getId(), ipAddress, LoginOutcome.LOCKED);
		}
		if (!passwordMatches(request.password(), user.getPasswordHash())) {
			user.registerFailedLogin(properties.login().maxFailedAttempts(), properties.login().lockDuration(), now);
			throw failure(email, user.getId(), ipAddress, LoginOutcome.BAD_CREDENTIALS);
		}
		user.registerSuccessfulLogin();
		loginEventRepository.save(new LoginEvent(email, user.getId(), ipAddress, LoginOutcome.SUCCESS));
		return issueTokens(user, now);
	}

	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public IssuedTokens refresh(String rawRefreshToken) {
		Instant now = Instant.now();
		RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
			.orElseThrow(InvalidRefreshTokenException::new);
		if (token.isRevoked()) {
			// Un jeton déjà utilisé revient : il a sans doute été volé, on coupe toutes les sessions de l'utilisateur
			refreshTokenRepository.revokeAllForUser(token.getUser().getId(), now);
			throw new InvalidRefreshTokenException();
		}
		if (token.isExpired(now)) {
			throw new InvalidRefreshTokenException();
		}
		// Rotation : chaque refresh token ne sert qu'une seule fois
		token.revoke(now);
		return issueTokens(token.getUser(), now);
	}

	public void logout(String rawRefreshToken) {
		refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken)).ifPresent((token) -> token.revoke(Instant.now()));
	}

	@Transactional(readOnly = true)
	public PageResponse<LoginEventResponse> findLoginEvents(Pageable pageable) {
		return PageResponse.from(loginEventRepository.findAll(pageable).map(LoginEventResponse::from));
	}

	private IssuedTokens issueTokens(User user, Instant now) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.jwt().issuer())
			.issuedAt(now)
			.expiresAt(now.plus(properties.jwt().accessTokenTtl()))
			.subject(user.getId().toString())
			.claim("email", user.getEmail())
			.claim("roles", List.of(user.getRole().name()))
			.build();
		String accessToken = jwtEncoder
			.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();

		String refreshToken = randomToken();
		refreshTokenRepository
			.save(new RefreshToken(user, sha256(refreshToken), now.plus(properties.jwt().refreshTokenTtl())));
		return new IssuedTokens(accessToken, properties.jwt().accessTokenTtl().toSeconds(), refreshToken);
	}

	private InvalidCredentialsException failure(String email, Long userId, String ipAddress, LoginOutcome outcome) {
		loginEventRepository.save(new LoginEvent(email, userId, ipAddress, outcome));
		return new InvalidCredentialsException();
	}

	// Toujours un calcul BCrypt (même sans compte) ; jamais plus de 72 octets, sinon BCrypt lèverait une exception
	private boolean passwordMatches(String rawPassword, String passwordHash) {
		if (tooLongForBcrypt(rawPassword)) {
			return false;
		}
		boolean matches = passwordEncoder.matches(rawPassword, (passwordHash != null) ? passwordHash : dummyPasswordHash);
		return matches && passwordHash != null;
	}

	private static boolean tooLongForBcrypt(String password) {
		return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
	}

	private static String normalize(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	private String randomToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String sha256(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 indisponible", ex);
		}
	}

}
