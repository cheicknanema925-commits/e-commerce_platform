package com.boutique.security;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Réglages « boutique.security.* » de application.yml, regroupés dans un objet typé et validé au démarrage :
// une clé JWT absente ou trop courte empêche l'application de démarrer
@Validated
@ConfigurationProperties("boutique.security")
public record SecurityProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull Login login, @Valid @NotNull RateLimit rateLimit,
		@NotNull Admin admin, @NotNull Keycloak keycloak, @NotNull RefreshCookie refreshCookie) {

	// secure = true : le cookie ne voyage qu'en HTTPS (désactivé seulement en développement, sur http://localhost)
	public record RefreshCookie(boolean secure) {
	}

	public record Jwt(@NotBlank @Size(min = 32, message = "doit contenir au moins 32 caractères (256 bits)") String secret,
			@NotBlank String issuer, @NotNull Duration accessTokenTtl, @NotNull Duration refreshTokenTtl) {
	}

	public record Login(@Positive int maxFailedAttempts, @NotNull Duration lockDuration) {
	}

	public record RateLimit(@Positive int requestsPerMinute) {
	}

	public record Admin(String email, String password) {
	}

	// issuerUri vide = Keycloak désactivé : seuls les jetons émis par l'API elle-même sont acceptés
	public record Keycloak(String issuerUri, @NotBlank String audience) {

		public boolean enabled() {
			return issuerUri != null && !issuerUri.isBlank();
		}

	}

}
