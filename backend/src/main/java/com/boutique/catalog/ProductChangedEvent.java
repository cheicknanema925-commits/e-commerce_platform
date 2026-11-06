package com.boutique.catalog;

// Un produit a été créé, modifié ou supprimé. Le catalogue l'annonce sans savoir qui écoute
// (aujourd'hui : l'index de recherche sémantique de l'agent)
public record ProductChangedEvent(Long productId, boolean deleted) {
}
