package com.boutique.cart;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

	// Panier + lignes + produits en une seule requête (pas de N+1 à l'affichage)
	@EntityGraph(attributePaths = { "items", "items.product" })
	Optional<Cart> findByUserId(Long userId);

}
