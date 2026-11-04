package com.boutique.order;

import com.boutique.common.ConflictException;
import com.boutique.common.NotFoundException;

// Exceptions des livreurs regroupées : petites classes sans logique
final class CourierExceptions {

	private CourierExceptions() {
	}

	static class CourierNotFoundException extends NotFoundException {

		CourierNotFoundException(Long id) {
			super("Livreur introuvable : " + id);
		}

	}

	static class InactiveCourierException extends ConflictException {

		InactiveCourierException(String name) {
			super(name + " est désactivé : il ne peut plus recevoir de commande.");
		}

	}

	static class CourierLockedException extends ConflictException {

		CourierLockedException(Long orderId) {
			super("La commande n° " + orderId + " est terminée : son livreur ne peut plus être changé.");
		}

	}

}
