package com.boutique.catalog;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id")
	private Category category;

	private String name;

	private String description;

	private BigDecimal price;

	private int stock;

	// Verrouillage optimiste : Hibernate vérifie et incrémente ce numéro à chaque UPDATE
	@Version
	private long version;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	protected Product() {
	}

	public Product(Category category, String name, String description, BigDecimal price, int stock) {
		update(category, name, description, price, stock);
	}

	public void update(Category category, String name, String description, BigDecimal price, int stock) {
		this.category = category;
		this.name = name;
		this.description = description;
		this.price = price;
		this.stock = stock;
	}

	public void removeStock(int quantity) {
		if (quantity > stock) {
			throw new InsufficientStockException(id, stock, quantity);
		}
		stock -= quantity;
	}

	// Remise en stock (commande annulée)
	public void addStock(int quantity) {
		stock += quantity;
	}

	public Long getId() {
		return id;
	}

	public Category getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getStock() {
		return stock;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
