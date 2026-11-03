package com.boutique.common;

// Base de toutes les erreurs « ressource introuvable » : traduites en 404 par GlobalExceptionHandler
public abstract class NotFoundException extends RuntimeException {

	protected NotFoundException(String message) {
		super(message);
	}

}
