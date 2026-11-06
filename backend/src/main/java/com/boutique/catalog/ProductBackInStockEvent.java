package com.boutique.catalog;

// Le stock d'un produit vient de repasser de 0 à plus de 0 (réassort par l'admin ou commande annulée)
public record ProductBackInStockEvent(Long productId) {
}
