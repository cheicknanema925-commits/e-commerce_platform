package com.boutique.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

// Limite les tentatives sur POST /api/auth/login par adresse IP (fenêtre fixe d'une minute) contre la force brute.
// Stockage en mémoire : suffisant pour un seul serveur ; avec plusieurs instances il faudrait un stockage partagé (Redis).
class LoginRateLimitFilter extends OncePerRequestFilter {

	private static final Duration WINDOW = Duration.ofMinutes(1);

	private static final int MAX_TRACKED_ADDRESSES = 10_000;

	private final int maxAttemptsPerWindow;

	private final SecurityProblemHandler problemHandler;

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	LoginRateLimitFilter(int maxAttemptsPerWindow, SecurityProblemHandler problemHandler) {
		this.maxAttemptsPerWindow = maxAttemptsPerWindow;
		this.problemHandler = problemHandler;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !("POST".equals(request.getMethod()) && "/api/auth/login".equals(request.getRequestURI()));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Instant now = Instant.now();
		if (windows.size() > MAX_TRACKED_ADDRESSES) {
			windows.values().removeIf((window) -> window.isOver(now));
		}
		Window window = windows.compute(request.getRemoteAddr(),
				(address, current) -> (current == null || current.isOver(now)) ? new Window(now, 1)
						: current.increment());
		if (window.count() > maxAttemptsPerWindow) {
			long retryAfter = Duration.between(now, window.start().plus(WINDOW)).toSeconds() + 1;
			response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
			problemHandler.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives",
					"Trop de tentatives de connexion. Réessayez dans " + retryAfter + " secondes.");
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
