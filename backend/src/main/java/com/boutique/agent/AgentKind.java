package com.boutique.agent;

// Deux agents séparés : celui des clients et celui des administrateurs (outils et prompt différents)
public enum AgentKind {

	CLIENT, ADMIN;

	// Identifiant de la conversation en mémoire (36 caractères maximum, contrainte de la table Spring AI)
	public String conversationId(Long userId) {
		return name().toLowerCase() + "-" + userId;
	}

}
