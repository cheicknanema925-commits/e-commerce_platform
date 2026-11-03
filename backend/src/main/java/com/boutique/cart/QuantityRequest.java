package com.boutique.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record QuantityRequest(@Positive @Max(Cart.MAX_QUANTITY_PER_PRODUCT) int quantity) {

}
