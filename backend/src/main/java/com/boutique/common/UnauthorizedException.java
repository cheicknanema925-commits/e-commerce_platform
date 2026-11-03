package com.boutique.common;

// Base des erreurs « identité non prouvée » (mauvais identifiants, jeton invalide) : traduites en 401
public abstract class UnauthorizedException extends RuntimeException {

	protected UnauthorizedException(String message) {
		super(message);
	}

}
