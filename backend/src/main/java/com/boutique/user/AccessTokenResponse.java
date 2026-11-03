package com.boutique.user;

// expiresIn : durée de validité du jeton d'accès, en secondes. Pas de refresh token ici : il est dans un cookie httpOnly.
public record AccessTokenResponse(String accessToken, String tokenType, long expiresIn) {

	static AccessTokenResponse from(IssuedTokens tokens) {
		return new AccessTokenResponse(tokens.accessToken(), "Bearer", tokens.expiresIn());
	}

}
