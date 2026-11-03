package com.boutique.user;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.security.SecurityProperties;

// Le jeton d'accès (15 min) part dans le corps de la réponse : le frontend le garde en mémoire.
// Le refresh token (7 jours) part UNIQUEMENT dans un cookie httpOnly : le JavaScript ne peut jamais le lire.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	static final String REFRESH_COOKIE = "boutique_refresh";

	private final AuthService authService;

	private final SecurityProperties properties;

	public AuthController(AuthService authService, SecurityProperties properties) {
		this.authService = authService;
		this.properties = properties;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		return withRefreshCookie(authService.login(request, httpRequest.getRemoteAddr()));
	}

	// Le navigateur envoie le cookie tout seul (même site) : le frontend appelle cette route au chargement
	// de la page et quand le jeton d'accès expire
	@PostMapping("/refresh")
	public ResponseEntity<AccessTokenResponse> refresh(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
		if (refreshToken == null || refreshToken.length() > 100) {
			throw new InvalidRefreshTokenException();
		}
		return withRefreshCookie(authService.refresh(refreshToken));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
		if (refreshToken != null && refreshToken.length() <= 100) {
			authService.logout(refreshToken);
		}
		// Max-Age = 0 : le navigateur supprime le cookie
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString()).build();
	}

	private ResponseEntity<AccessTokenResponse> withRefreshCookie(IssuedTokens tokens) {
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE,
					refreshCookie(tokens.refreshToken(), properties.jwt().refreshTokenTtl()).toString())
			.body(AccessTokenResponse.from(tokens));
	}

	private ResponseCookie refreshCookie(String value, Duration maxAge) {
		return ResponseCookie.from(REFRESH_COOKIE, value)
			// Illisible par le JavaScript : une faille XSS ne permet pas de le voler
			.httpOnly(true)
			// HTTPS uniquement (sauf en développement local)
			.secure(properties.refreshCookie().secure())
			// Jamais envoyé par une requête venant d'un autre site : protège du CSRF
			.sameSite("Strict")
			// Envoyé seulement aux routes d'authentification, pas à chaque appel de l'API
			.path("/api/auth")
			.maxAge(maxAge)
			.build();
	}

}
