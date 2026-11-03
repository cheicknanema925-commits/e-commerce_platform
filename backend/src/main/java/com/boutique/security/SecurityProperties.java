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
public record SecurityProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull Login login, @NotNull Admin admin) {

	public record Jwt(@NotBlank @Size(min = 32, message = "doit contenir au moins 32 caractères (256 bits)") String secret,
			@NotBlank String issuer, @NotNull Duration accessTokenTtl, @NotNull Duration refreshTokenTtl) {
	}

	public record Login(@Positive int maxFailedAttempts, @NotNull Duration lockDuration,
			@Positive int maxAttemptsPerMinute) {
	}

	public record Admin(String email, String password) {
	}

}
