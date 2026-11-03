package com.boutique.payment;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.boutique.payment.PaymentExceptions.InvalidWebhookException;
import com.boutique.payment.PaymentExceptions.PaymentProviderException;

// Faux fournisseur pour le développement et les tests : aucun argent réel, transactions gardées en mémoire.
// Il imite un vrai fournisseur, y compris la signature des webhooks (HMAC-SHA256 dans l'en-tête X-Fake-Signature).
// Refusé en production (voir PaymentConfig).
public class FakePaymentProvider implements PaymentProvider {

	static final String SIGNATURE_HEADER = "X-Fake-Signature";

	private final Map<String, VerifiedPayment> transactions = new ConcurrentHashMap<>();

	private final byte[] webhookSecret;

	private final String checkoutBaseUrl;

	public FakePaymentProvider(String webhookSecret, String checkoutBaseUrl) {
		this.webhookSecret = webhookSecret.getBytes(StandardCharsets.UTF_8);
		this.checkoutBaseUrl = checkoutBaseUrl;
	}

	@Override
	public String name() {
		return "fake";
	}

	@Override
	public PaymentSession initiate(PaymentRequest request) {
		String reference = "FAKE-" + UUID.randomUUID();
		transactions.put(reference, new VerifiedPayment(ProviderStatus.PENDING, request.amount()));
		return new PaymentSession(reference, checkoutBaseUrl + reference);
	}

	@Override
	public VerifiedPayment verify(String reference) {
		VerifiedPayment payment = transactions.get(reference);
		if (payment == null) {
			throw new PaymentProviderException("Transaction inconnue du faux fournisseur : " + reference);
		}
		return payment;
	}

	@Override
	public WebhookNotification parseWebhook(WebhookRequest request) {
		String reference = request.parameter("reference");
		String signature = request.header(SIGNATURE_HEADER);
		if (reference == null || signature == null || !MessageDigest.isEqual(
				signature.getBytes(StandardCharsets.UTF_8), sign(reference).getBytes(StandardCharsets.UTF_8))) {
			throw new InvalidWebhookException();
		}
		return new WebhookNotification(reference);
	}

	// --- Outils de simulation (tests et profil dev) : « le client a payé / annulé sur la page du fournisseur » ---

	public void simulate(String reference, ProviderStatus status) {
		simulate(reference, status, verify(reference).amount());
	}

	// Permet de simuler un montant payé différent de celui de la commande (tentative de fraude)
	public void simulate(String reference, ProviderStatus status, BigDecimal paidAmount) {
		transactions.put(reference, new VerifiedPayment(status, paidAmount));
	}

	public WebhookRequest signedWebhook(String reference) {
		return new WebhookRequest(Map.of("reference", reference),
				Map.of(SIGNATURE_HEADER.toLowerCase(java.util.Locale.ROOT), sign(reference)));
	}

	public String sign(String reference) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(webhookSecret, "HmacSHA256"));
			return java.util.HexFormat.of().formatHex(mac.doFinal(reference.getBytes(StandardCharsets.UTF_8)));
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("HMAC-SHA256 indisponible", ex);
		}
	}

}
