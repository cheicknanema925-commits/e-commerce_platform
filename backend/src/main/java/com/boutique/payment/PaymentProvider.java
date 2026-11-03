package com.boutique.payment;

import java.math.BigDecimal;
import java.util.Map;

// Pattern Stratégie : le reste de l'application ne parle qu'à cette interface. Changer de fournisseur
// (faux fournisseur en dev/test, PayDunya, demain un autre) ne touche ni PaymentService ni les commandes.
public interface PaymentProvider {

	// Identifiant utilisé dans l'URL du webhook : /api/payments/webhooks/{name}
	String name();

	// 1. Initier : crée la transaction chez le fournisseur et renvoie l'URL de SA page de paiement
	PaymentSession initiate(PaymentRequest request);

	// 2. Vérifier : demande au fournisseur le VRAI statut et le VRAI montant (source de vérité)
	VerifiedPayment verify(String reference);

	// 3. Traiter un webhook : vérifie qu'il vient bien du fournisseur et extrait la référence de la transaction
	WebhookNotification parseWebhook(WebhookRequest request);

	record PaymentRequest(Long orderId, BigDecimal amount, String description, String returnUrl, String cancelUrl,
			String callbackUrl) {
	}

	record PaymentSession(String reference, String checkoutUrl) {
	}

	record VerifiedPayment(ProviderStatus status, BigDecimal amount) {
	}

	enum ProviderStatus {

		PENDING, SUCCEEDED, FAILED

	}

	record WebhookNotification(String reference) {
	}

	// Paramètres de formulaire et en-têtes (noms en minuscules) de la requête reçue
	record WebhookRequest(Map<String, String> parameters, Map<String, String> headers) {

		public String parameter(String name) {
			return parameters.get(name);
		}

		public String header(String name) {
			return headers.get(name.toLowerCase(java.util.Locale.ROOT));
		}

	}

}
