package com.boutique.support;

import java.time.Instant;

import jakarta.persistence.Column;
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

import com.boutique.user.User;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {

	public enum Category {

		ORDER, DELIVERY, PAYMENT, PRODUCT, OTHER

	}

	public enum Status {

		OPEN, ANSWERED, CLOSED

	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	// Simple identifiant : le ticket n'a pas besoin de charger la commande (propriété vérifiée à la création)
	@Column(name = "order_id")
	private Long orderId;

	@Enumerated(EnumType.STRING)
	private Category category;

	private String subject;

	private String message;

	@Enumerated(EnumType.STRING)
	private Status status = Status.OPEN;

	private String reply;

	@CreationTimestamp
	private Instant createdAt;

	private Instant repliedAt;

	protected SupportTicket() {
	}

	SupportTicket(User user, Long orderId, Category category, String subject, String message) {
		this.user = user;
		this.orderId = orderId;
		this.category = category;
		this.subject = subject;
		this.message = message;
	}

	void answer(String reply, Instant now) {
		if (status == Status.CLOSED) {
			throw new TicketClosedException(id);
		}
		this.reply = reply;
		this.status = Status.ANSWERED;
		this.repliedAt = now;
	}

	void close() {
		status = Status.CLOSED;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Long getOrderId() {
		return orderId;
	}

	public Category getCategory() {
		return category;
	}

	public String getSubject() {
		return subject;
	}

	public String getMessage() {
		return message;
	}

	public Status getStatus() {
		return status;
	}

	public String getReply() {
		return reply;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getRepliedAt() {
		return repliedAt;
	}

}
