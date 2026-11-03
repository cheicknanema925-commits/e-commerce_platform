package com.boutique.order;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeliveryAddressRequest(
		@NotBlank @Size(max = 255) String addressLine,
		@NotBlank @Size(max = 100) String city,
		@NotBlank @Pattern(regexp = "^\\+?[0-9 ]{9,20}$", message = "numéro de téléphone invalide") String phone,
		@DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6) BigDecimal latitude,
		@DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6) BigDecimal longitude) {

}
