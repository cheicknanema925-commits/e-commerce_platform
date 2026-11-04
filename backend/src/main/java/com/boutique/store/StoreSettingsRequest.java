package com.boutique.store;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record StoreSettingsRequest(@NotBlank @Size(max = 100) String storeName,
		@NotBlank @Email @Size(max = 255) String supportEmail, boolean onlinePaymentEnabled, boolean codEnabled,
		@Positive BigDecimal codMaxAmount, @NotNull @PositiveOrZero BigDecimal codFee,
		@NotNull @Size(max = 30) List<@NotBlank @Size(max = 60) String> codZones) {
}
