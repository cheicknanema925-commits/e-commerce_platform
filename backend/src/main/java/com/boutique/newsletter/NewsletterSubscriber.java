package com.boutique.newsletter;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "newsletter_subscribers")
public class NewsletterSubscriber {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String email;

	private String unsubscribeToken;

	private Instant createdAt;

	protected NewsletterSubscriber() {
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	// Package-private : le jeton ne sort jamais du module (ni dans l'API admin, ni ailleurs)
	String getUnsubscribeToken() {
		return unsubscribeToken;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
