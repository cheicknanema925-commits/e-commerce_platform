package com.boutique.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Aucun champ « role » : un client ne peut pas choisir de s'inscrire comme ADMIN
public record RegisterRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 150) String fullName,
		@NotBlank @Size(min = 12, max = 72) String password) {

}
