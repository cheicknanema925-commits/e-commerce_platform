package com.boutique.security;

import org.springframework.security.oauth2.jwt.Jwt;

// L'identité de l'utilisateur vient du jeton signé (claim « sub »), jamais d'un paramètre envoyé par le client
public final class CurrentUser {

	private CurrentUser() {
	}

	public static Long id(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}

}
