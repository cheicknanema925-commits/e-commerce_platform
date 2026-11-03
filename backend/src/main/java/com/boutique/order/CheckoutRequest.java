package com.boutique.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

// Le client choisit l'adresse et le mode de paiement ; les produits viennent du panier, les prix de la base
public record CheckoutRequest(@NotNull @Valid DeliveryAddressRequest delivery, @NotNull PaymentMethod paymentMethod) {

}
