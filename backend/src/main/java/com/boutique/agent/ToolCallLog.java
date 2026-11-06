package com.boutique.agent;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

// Journal d'un appel d'outil : qui, quel outil, avec quels arguments, quel résultat, combien de temps
@Entity
@Table(name = "agent_tool_calls")
public class ToolCallLog {

	public enum Outcome {

		OK, REFUSED, ERROR

	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long userId;

	@Enumerated(EnumType.STRING)
	private AgentKind agent;

	private String tool;

	private String arguments;

	@Enumerated(EnumType.STRING)
	private Outcome outcome;

	private String detail;

	private int durationMs;

	@CreationTimestamp
	private Instant createdAt;

	protected ToolCallLog() {
	}

	ToolCallLog(Long userId, AgentKind agent, String tool, String arguments, Outcome outcome, String detail,
			int durationMs) {
		this.userId = userId;
		this.agent = agent;
		this.tool = tool;
		this.arguments = truncate(arguments, 1000);
		this.outcome = outcome;
		this.detail = truncate(detail, 500);
		this.durationMs = durationMs;
	}

	private static String truncate(String text, int max) {
		return (text == null || text.length() <= max) ? text : text.substring(0, max);
	}

	public Long getId() {
		return id;
	}

	public String getTool() {
		return tool;
	}

	public Outcome getOutcome() {
		return outcome;
	}

	public Long getUserId() {
		return userId;
	}

	public AgentKind getAgent() {
		return agent;
	}

	public String getArguments() {
		return arguments;
	}

}
