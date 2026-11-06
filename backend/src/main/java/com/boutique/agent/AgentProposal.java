package com.boutique.agent;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Action préparée par un agent, en attente de la confirmation de l'utilisateur (bouton « Confirmer »)
@Entity
@Table(name = "agent_proposals")
public class AgentProposal {

	public enum Status {

		PENDING, CONFIRMED, CANCELLED, EXPIRED

	}

	@Id
	private UUID id;

	private Long userId;

	@Enumerated(EnumType.STRING)
	private AgentKind agent;

	// Étapes en JSON (liste de ProposalStep)
	private String steps;

	@Enumerated(EnumType.STRING)
	private Status status;

	private String results;

	private Instant createdAt;

	private Instant expiresAt;

	private Instant decidedAt;

	protected AgentProposal() {
	}

	AgentProposal(Long userId, AgentKind agent, String steps, Instant now, Instant expiresAt) {
		this.id = UUID.randomUUID();
		this.userId = userId;
		this.agent = agent;
		this.steps = steps;
		this.status = Status.PENDING;
		this.createdAt = now;
		this.expiresAt = expiresAt;
	}

	void finish(String results, Instant now) {
		this.results = results;
		this.decidedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public AgentKind getAgent() {
		return agent;
	}

	public String getSteps() {
		return steps;
	}

	public Status getStatus() {
		return status;
	}

	public String getResults() {
		return results;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

}
