package com.boutique.common;

// Base des erreurs « la requête est incorrecte » détectées par le code métier : traduites en 400
public abstract class BadRequestException extends RuntimeException {

	protected BadRequestException(String message) {
		super(message);
	}

}
