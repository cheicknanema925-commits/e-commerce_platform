package com.boutique.security;

import java.io.IOException;
import java.net.URI;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

// Les refus de Spring Security arrivent AVANT les contrôleurs, donc hors de GlobalExceptionHandler :
// cette classe leur donne le même format ProblemDetail que le reste de l'API
class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	// Ajoutent l'en-tête standard WWW-Authenticate (RFC 6750), puis on écrit le corps
	private final AuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();

	private final AccessDeniedHandler bearerAccessDeniedHandler = new BearerTokenAccessDeniedHandler();

	private final JsonMapper jsonMapper;

	SecurityProblemHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		bearerEntryPoint.commence(request, response, authException);
		write(request, response, HttpStatus.UNAUTHORIZED, "Non authentifié",
				"Authentification requise : jeton absent, invalide ou expiré.");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		bearerAccessDeniedHandler.handle(request, response, accessDeniedException);
		write(request, response, HttpStatus.FORBIDDEN, "Accès refusé", "Vous n'avez pas les droits nécessaires.");
	}

	void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String title,
			String detail) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setInstance(URI.create(request.getRequestURI()));
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), problem);
	}

}
