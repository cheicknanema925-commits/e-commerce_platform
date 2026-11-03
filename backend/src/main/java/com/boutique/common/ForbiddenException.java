package com.boutique.common;

// Base des erreurs « tu es bien identifié, mais une règle métier t'interdit cette action » : traduites en 403
public abstract class ForbiddenException extends RuntimeException {

	protected ForbiddenException(String message) {
		super(message);
	}

}
