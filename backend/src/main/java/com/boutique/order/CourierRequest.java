package com.boutique.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CourierRequest(@NotBlank @Size(max = 100) String name,
		@NotBlank @Pattern(regexp = "^\\+?[0-9 ]{9,20}$", message = "numéro de téléphone invalide") String phone,
		boolean active) {
}
