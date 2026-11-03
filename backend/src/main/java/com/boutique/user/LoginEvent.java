package com.boutique.user;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "login_events")
public class LoginEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String email;

	private Long userId;

	private String ipAddress;

	@Enumerated(EnumType.STRING)
	private LoginOutcome outcome;

	@CreationTimestamp
	private Instant createdAt;

	protected LoginEvent() {
	}

	public LoginEvent(String email, Long userId, String ipAddress, LoginOutcome outcome) {
		this.email = email;
		this.userId = userId;
		this.ipAddress = ipAddress;
		this.outcome = outcome;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public Long getUserId() {
		return userId;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public LoginOutcome getOutcome() {
		return outcome;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
