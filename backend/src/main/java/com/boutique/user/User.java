package com.boutique.user;

import java.time.Duration;
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
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String email;

	private String fullName;

	@Enumerated(EnumType.STRING)
	private Role role;

	private String passwordHash;

	private int failedLoginAttempts;

	private Instant lockedUntil;

	@CreationTimestamp
	private Instant createdAt;

	protected User() {
	}

	public User(String email, String fullName, Role role) {
		this(email, fullName, role, null);
	}

	public User(String email, String fullName, Role role, String passwordHash) {
		this.email = email;
		this.fullName = fullName;
		this.role = role;
		this.passwordHash = passwordHash;
	}

	public boolean isLocked(Instant now) {
		return lockedUntil != null && now.isBefore(lockedUntil);
	}

	// Après maxAttempts échecs consécutifs, le compte est verrouillé pendant lockDuration
	public void registerFailedLogin(int maxAttempts, Duration lockDuration, Instant now) {
		failedLoginAttempts++;
		if (failedLoginAttempts >= maxAttempts) {
			lockedUntil = now.plus(lockDuration);
			failedLoginAttempts = 0;
		}
	}

	public void registerSuccessfulLogin() {
		failedLoginAttempts = 0;
		lockedUntil = null;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getFullName() {
		return fullName;
	}

	public Role getRole() {
		return role;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
