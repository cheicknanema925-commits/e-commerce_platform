package com.boutique.user;

// expiresIn : durée de validité du jeton d'accès, en secondes
public record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken) {

}
