package com.boutique.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

// Seule l'adresse de livraison vient du client : les produits viennent du panier, les prix de la base
public record CheckoutRequest(@NotNull @Valid DeliveryAddressRequest delivery) {

}
