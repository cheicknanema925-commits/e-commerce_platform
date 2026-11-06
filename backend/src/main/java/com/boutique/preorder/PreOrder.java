package com.boutique.preorder;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.boutique.catalog.Product;
import com.boutique.user.User;

// « Prévenez-moi quand il revient » : ni paiement ni stock réservé, seulement une demande à notifier
@Entity
@Table(name = "pre_orders")
public class PreOrder {

	public enum Status {

		WAITING, NOTIFIED, CANCELLED

	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	private int quantity;

	@Enumerated(EnumType.STRING)
	private Status status = Status.WAITING;

	@CreationTimestamp
	private Instant createdAt;

	private Instant notifiedAt;

	protected PreOrder() {
	}

	PreOrder(User user, Product product, int quantity) {
		this.user = user;
		this.product = product;
		this.quantity = quantity;
	}

	void changeQuantity(int quantity) {
		this.quantity = quantity;
	}

	void cancel() {
		if (status != Status.WAITING) {
			throw new PreOrderNotWaitingException(id);
		}
		status = Status.CANCELLED;
	}

	void markNotified(Instant now) {
		status = Status.NOTIFIED;
		notifiedAt = now;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Product getProduct() {
		return product;
	}

	public int getQuantity() {
		return quantity;
	}

	public Status getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getNotifiedAt() {
		return notifiedAt;
	}

}
