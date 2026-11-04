package com.boutique.catalog;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// brand, sku et compareAtPrice sont facultatifs ; sku vide = référence générée par le serveur
public record ProductRequest(
		@NotNull Long categoryId,
		@NotBlank @Size(max = 100) String name,
		@Size(max = 1000) String description,
		@NotNull @Positive BigDecimal price,
		@NotNull @PositiveOrZero Integer stock,
		@Size(max = 80) String brand,
		@Pattern(regexp = "[A-Za-z0-9._-]{1,40}", message = "lettres, chiffres, point, tiret ou souligné (40 au maximum)") String sku,
		@Positive BigDecimal compareAtPrice) {

}
