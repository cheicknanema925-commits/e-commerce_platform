package com.boutique.newsletter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubscriptionRequest(@NotBlank @Email @Size(max = 255) String email) {
}
