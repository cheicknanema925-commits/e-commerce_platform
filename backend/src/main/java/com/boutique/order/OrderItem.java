package com.boutique.order;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.boutique.catalog.Product;

@Entity
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	private String productName;

	private BigDecimal unitPrice;

	private int quantity;

	protected OrderItem() {
	}

	OrderItem(Order order, Product product, int quantity) {
		this.order = order;
		this.product = product;
		// Copie au moment de l'achat : un changement de prix du produit ne modifie pas la commande
		this.productName = product.getName();
		this.unitPrice = product.getPrice();
		this.quantity = quantity;
	}

	public BigDecimal getSubtotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public String getProductName() {
		return productName;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public int getQuantity() {
		return quantity;
	}

}
