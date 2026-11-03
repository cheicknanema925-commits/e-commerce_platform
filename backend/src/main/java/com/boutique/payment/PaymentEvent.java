package com.boutique.payment;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

// Journal : chaque étape d'un paiement laisse une trace (utile en cas de litige ou d'enquête)
@Entity
@Table(name = "payment_events")
public class PaymentEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Nul quand l'événement ne correspond à aucun paiement connu (ex. webhook rejeté)
	private Long paymentId;

	private String provider;

	@Enumerated(EnumType.STRING)
	private PaymentEventType type;

	private String detail;

	@CreationTimestamp
	private Instant createdAt;

	protected PaymentEvent() {
	}

	public PaymentEvent(Long paymentId, String provider, PaymentEventType type, String detail) {
		this.paymentId = paymentId;
		this.provider = provider;
		this.type = type;
		this.detail = (detail != null && detail.length() > 1000) ? detail.substring(0, 1000) : detail;
	}

	public Long getId() {
		return id;
	}

	public Long getPaymentId() {
		return paymentId;
	}

	public String getProvider() {
		return provider;
	}

	public PaymentEventType getType() {
		return type;
	}

	public String getDetail() {
		return detail;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
