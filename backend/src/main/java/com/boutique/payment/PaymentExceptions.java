package com.boutique.payment;

import com.boutique.common.ConflictException;
import com.boutique.common.ExternalServiceException;
import com.boutique.common.NotFoundException;
import com.boutique.common.UnauthorizedException;

// Les exceptions du module paiement, regroupées : chacune hérite de la base qui fixe son code HTTP
final class PaymentExceptions {

	private PaymentExceptions() {
	}

	static class PaymentNotAllowedException extends ConflictException {

		PaymentNotAllowedException() {
			super("Cette commande ne peut pas (ou plus) être payée en ligne.");
		}

	}

	static class InvalidWebhookException extends UnauthorizedException {

		InvalidWebhookException() {
			super("Notification de paiement refusée : origine non vérifiée.");
		}

	}

	static class PaymentNotFoundException extends NotFoundException {

		PaymentNotFoundException(String reference) {
			super("Paiement introuvable : " + reference);
		}

	}

	static class UnknownProviderException extends NotFoundException {

		UnknownProviderException(String provider) {
			super("Fournisseur de paiement inconnu : " + provider);
		}

	}

	static class PaymentProviderException extends ExternalServiceException {

		PaymentProviderException(String message) {
			super(message);
		}

		PaymentProviderException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
