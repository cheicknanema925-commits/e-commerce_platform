package com.boutique.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Aucun champ « prix » : le prix vient toujours de la base, jamais du client
public record CartItemRequest(@NotNull Long productId, @Positive @Max(Cart.MAX_QUANTITY_PER_PRODUCT) int quantity) {

}
