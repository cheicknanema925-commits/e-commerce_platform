package com.boutique.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Tailles bornées : sans elles, un email de plusieurs Mo faisait échouer l'écriture du journal d'audit (audit contourné)
public record LoginRequest(@NotBlank @Size(max = 255) String email, @NotBlank @Size(max = 128) String password) {

}
