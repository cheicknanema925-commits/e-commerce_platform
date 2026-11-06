package com.boutique.agent;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.boutique.common.TooManyRequestsException;

// Nombre de messages par utilisateur et par minute (fenêtre fixe, en mémoire : suffisant pour un seul serveur).
// Un modèle de langage consomme beaucoup de calcul : on empêche qu'un seul compte sature l'assistant.
@Component
class AgentRateLimiter {

	private static final Duration WINDOW = Duration.ofMinutes(1);

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	private final AgentProperties properties;

	AgentRateLimiter(AgentProperties properties) {
		this.properties = properties;
	}

	void check(AgentKind agent, Long userId) {
		Instant now = Instant.now();
		Window window = windows.compute(agent.conversationId(userId),
				(key, current) -> (current == null || !now.isBefore(current.start().plus(WINDOW))) ? new Window(now, 1)
						: new Window(current.start(), current.count() + 1));
		if (window.count() > properties.messagesPerMinute()) {
			throw new TooManyRequestsException("Trop de messages en peu de temps. Réessayez dans une minute.");
		}
	}

	private record Window(Instant start, int count) {
	}

}
