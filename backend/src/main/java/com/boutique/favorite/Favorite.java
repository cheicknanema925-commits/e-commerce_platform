package com.boutique.favorite;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.boutique.catalog.Product;

// Clé primaire composée (user_id, product_id) : la base elle-même interdit un doublon
@Entity
@Table(name = "favorites")
public class Favorite {

	@EmbeddedId
	private Key id;

	// @MapsId : la colonne product_id sert à la fois de clé et de relation vers le produit
	@MapsId("productId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@CreationTimestamp
	private Instant createdAt;

	protected Favorite() {
	}

	Favorite(Long userId, Product product) {
		this.id = new Key(userId, product.getId());
		this.product = product;
	}

	public Product getProduct() {
		return product;
	}

	@Embeddable
	public record Key(Long userId, Long productId) implements Serializable {
	}

}
