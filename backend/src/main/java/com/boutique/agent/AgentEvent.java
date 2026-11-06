package com.boutique.agent;

import java.util.List;
import java.util.Map;

// Événements envoyés au navigateur pendant une réponse (flux SSE) : le texte au fil de l'eau, puis les fiches
// produits lues EN BASE (prix et stock exacts), l'action à confirmer et les éventuels avertissements
public sealed interface AgentEvent {

	String type();

	record Token(String text) implements AgentEvent {

		public String type() {
			return "token";
		}

	}

	record Products(List<Map<String, Object>> products) implements AgentEvent {

		public String type() {
			return "products";
		}

	}

	record Proposal(ProposalView proposal) implements AgentEvent {

		public String type() {
			return "proposal";
		}

	}

	record Warning(String message) implements AgentEvent {

		public String type() {
			return "warning";
		}

	}

	record Failure(String message) implements AgentEvent {

		public String type() {
			return "error";
		}

	}

	record Done() implements AgentEvent {

		public String type() {
			return "done";
		}

	}

}
