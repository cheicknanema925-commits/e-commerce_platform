package com.boutique.common;

// Base des erreurs d'un service extérieur (agrégateur de paiement, modèle d'IA…) : traduites en 502.
// Le détail technique reste dans les logs, le client reçoit un message générique.
public abstract class ExternalServiceException extends RuntimeException {

	protected ExternalServiceException(String message) {
		super(message);
	}

	protected ExternalServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}
