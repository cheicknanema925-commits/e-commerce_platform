package com.boutique.common;

// Base des erreurs « la demande est valide mais contredit l'état actuel » : traduites en 409
public abstract class ConflictException extends RuntimeException {

	protected ConflictException(String message) {
		super(message);
	}

}
