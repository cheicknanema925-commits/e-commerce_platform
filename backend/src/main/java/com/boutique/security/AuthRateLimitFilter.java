package com.boutique.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

// Limite par adresse IP les requêtes sur la connexion (force brute) et l'inscription (comptes créés en masse,
// énumération des emails). Fenêtre fixe d'une minute, un compteur par route et par IP.
// Stockage en mémoire : suffisant pour un seul serveur ; avec plusieurs instances il faudrait un stockage partagé (Redis).
// Étape 9 : derrière Caddy, getRemoteAddr() sera l'IP du proxy → configurer les en-têtes X-Forwarded-For de confiance.
class AuthRateLimitFilter extends OncePerRequestFilter {

	private static final Set<String> PROTECTED_PATHS = Set.of("/api/auth/login", "/api/auth/register");

	private static final Duration WINDOW = Duration.ofMinutes(1);

	private static final int MAX_TRACKED_KEYS = 10_000;

	private final int maxRequestsPerWindow;

	private final SecurityProblemHandler problemHandler;

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	AuthRateLimitFilter(int maxRequestsPerWindow, SecurityProblemHandler problemHandler) {
		this.maxRequestsPerWindow = maxRequestsPerWindow;
		this.problemHandler = problemHandler;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !("POST".equals(request.getMethod()) && PROTECTED_PATHS.contains(request.getRequestURI()));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Instant now = Instant.now();
		if (windows.size() > MAX_TRACKED_KEYS) {
			windows.values().removeIf((window) -> window.isOver(now));
		}
		String key = request.getRequestURI() + "|" + request.getRemoteAddr();
		Window window = windows.compute(key,
				(k, current) -> (current == null || current.isOver(now)) ? new Window(now, 1) : current.increment());
		if (window.count() > maxRequestsPerWindow) {
			long retryAfter = Duration.between(now, window.start().plus(WINDOW)).toSeconds() + 1;
			response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
			problemHandler.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives",
					"Trop de tentatives. Réessayez dans " + retryAfter + " secondes.");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private record Window(Instant start, int count) {

		boolean isOver(Instant now) {
			return !now.isBefore(start.plus(WINDOW));
		}

		Window increment() {
			return new Window(start, count + 1);
		}

	}

}
