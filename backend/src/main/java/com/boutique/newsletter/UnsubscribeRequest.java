package com.boutique.newsletter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnsubscribeRequest(@NotBlank @Size(max = 64) String token) {
}
