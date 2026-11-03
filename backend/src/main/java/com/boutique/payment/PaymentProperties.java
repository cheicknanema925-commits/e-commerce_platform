package com.boutique.payment;

import java.net.URI;
import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("boutique.payment")
public record PaymentProperties(@NotBlank String provider, @NotNull URI publicApiUrl, @NotNull URI frontendUrl,
		@NotBlank String storeName, @Valid @NotNull Reconciliation reconciliation, @NotNull PayDunya paydunya,
		@Valid @NotNull Fake fake) {

	public record Reconciliation(boolean enabled, @NotNull Duration interval, @NotNull Duration pendingDelay) {
	}

	// mode : « test » (bac à sable) ou « live » ; clés lues dans l'environnement, jamais dans le code
	public record PayDunya(String mode, String masterKey, String privateKey, String token) {
	}

	public record Fake(@NotBlank String webhookSecret) {
	}

}
