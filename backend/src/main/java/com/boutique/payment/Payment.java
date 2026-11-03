package com.boutique.payment;

import java.math.BigDecimal;
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
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.boutique.order.Order;

@Entity
@Table(name = "payments")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	private String provider;

	private String providerReference;

	// Montant de la commande au moment du paiement, calculé par le serveur (jamais envoyé par le client)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	private PaymentStatus status;

	@Version
	private long version;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	protected Payment() {
	}

	public Payment(Order order, String provider, BigDecimal amount, String providerReference) {
		this.order = order;
		this.provider = provider;
		this.amount = amount;
		this.providerReference = providerReference;
		this.status = PaymentStatus.PENDING;
	}

	public boolean isPending() {
		return status == PaymentStatus.PENDING;
	}

	public void succeed() {
		status = PaymentStatus.SUCCEEDED;
	}

	public void fail() {
		status = PaymentStatus.FAILED;
	}

	public Long getId() {
		return id;
	}

	public Order getOrder() {
		return order;
	}

	public String getProvider() {
		return provider;
	}

	public String getProviderReference() {
		return providerReference;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
