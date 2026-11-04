package com.boutique.catalog;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "product_images")
public class ProductImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	// Nom aléatoire choisi par le serveur (UUID), jamais le nom du fichier envoyé
	private String filename;

	private int position;

	@CreationTimestamp
	private Instant createdAt;

	protected ProductImage() {
	}

	ProductImage(Product product, String filename, int position) {
		this.product = product;
		this.filename = filename;
		this.position = position;
	}

	void moveTo(int position) {
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public String getFilename() {
		return filename;
	}

	public int getPosition() {
		return position;
	}

}
