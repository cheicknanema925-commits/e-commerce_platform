package com.boutique.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.boutique.payment.PaymentExceptions.InvalidWebhookException;
import com.boutique.payment.PaymentExceptions.PaymentProviderException;

// Intégration PayDunya (API HTTP/JSON) écrite UNIQUEMENT d'après la documentation officielle :
// https://developers.paydunya.com/doc/FR/http_json et https://developers.paydunya.com/doc/FR/php
public class PayDunyaPaymentProvider implements PaymentProvider {

	static final String SANDBOX_BASE_URL = "https://app.paydunya.com/sandbox-api/v1";

	static final String LIVE_BASE_URL = "https://app.paydunya.com/api/v1";

	private final RestClient restClient;

	private final String storeName;

	// La doc : « Le hash renvoyé par PayDunya est le hash de votre MasterKey », algorithme SHA-512
	private final String expectedWebhookHash;

	public PayDunyaPaymentProvider(RestClient.Builder restClientBuilder, PaymentProperties.PayDunya config,
			String storeName) {
		if (!StringUtils.hasText(config.masterKey()) || !StringUtils.hasText(config.privateKey())
				|| !StringUtils.hasText(config.token())) {
			throw new IllegalStateException(
					"PayDunya : PAYDUNYA_MASTER_KEY, PAYDUNYA_PRIVATE_KEY et PAYDUNYA_TOKEN doivent être définies");
		}
		this.restClient = restClientBuilder.baseUrl("live".equals(config.mode()) ? LIVE_BASE_URL : SANDBOX_BASE_URL)
			.defaultHeader("PAYDUNYA-MASTER-KEY", config.masterKey())
			.defaultHeader("PAYDUNYA-PRIVATE-KEY", config.privateKey())
			.defaultHeader("PAYDUNYA-TOKEN", config.token())
			.build();
		this.storeName = storeName;
		this.expectedWebhookHash = sha512Hex(config.masterKey());
	}

	@Override
	public String name() {
		return "paydunya";
	}

	// POST /checkout-invoice/create → { response_code: "00", response_text: <URL de paiement>, token }
	@Override
	public PaymentSession initiate(PaymentRequest request) {
		// Le franc CFA n'a pas de centimes : un montant avec décimales est une erreur, on refuse plutôt que d'arrondir
		BigDecimal amount = request.amount().setScale(0, RoundingMode.UNNECESSARY);
		Map<String, Object> body = Map.of(
				"invoice", Map.of("total_amount", amount, "description", request.description()),
				"store", Map.of("name", storeName),
				"actions", Map.of("return_url", request.returnUrl(), "cancel_url", request.cancelUrl(), "callback_url",
						request.callbackUrl()),
				"custom_data", Map.of("order_id", request.orderId()));
		CreateInvoiceResponse response = call(() -> restClient.post()
			.uri("/checkout-invoice/create")
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.retrieve()
			.onStatus(HttpStatusCode::isError, (req, res) -> {
				throw new PaymentProviderException("PayDunya a répondu " + res.getStatusCode());
			})
			.body(CreateInvoiceResponse.class));
		if (response == null || !"00".equals(response.responseCode()) || !StringUtils.hasText(response.token())) {
			throw new PaymentProviderException("PayDunya a refusé la facture : "
					+ ((response != null) ? response.responseCode() + " " + response.description() : "réponse vide"));
		}
		return new PaymentSession(response.token(), response.responseText());
	}

	// GET /checkout-invoice/confirm/{token} → status : pending, completed, cancelled ou failed
	@Override
	public VerifiedPayment verify(String token) {
		ConfirmResponse response = call(() -> restClient.get()
			.uri("/checkout-invoice/confirm/{token}", token)
			.retrieve()
			.onStatus(HttpStatusCode::isError, (req, res) -> {
				throw new PaymentProviderException("PayDunya a répondu " + res.getStatusCode());
			})
			.body(ConfirmResponse.class));
		if (response == null || !"00".equals(response.responseCode()) || response.invoice() == null) {
			throw new PaymentProviderException("Facture PayDunya introuvable : " + token);
		}
		ProviderStatus status = switch (String.valueOf(response.status())) {
			case "completed" -> ProviderStatus.SUCCEEDED;
			case "cancelled", "failed" -> ProviderStatus.FAILED;
			default -> ProviderStatus.PENDING;
		};
		return new VerifiedPayment(status, response.invoice().totalAmount());
	}

	// IPN : POST application/x-www-form-urlencoded, champs data[hash], data[status], data[invoice][token]…
	@Override
	public WebhookNotification parseWebhook(WebhookRequest request) {
		String hash = request.parameter("data[hash]");
		String token = request.parameter("data[invoice][token]");
		if (hash == null || !StringUtils.hasText(token) || !MessageDigest.isEqual(
				hash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
				expectedWebhookHash.getBytes(StandardCharsets.UTF_8))) {
			throw new InvalidWebhookException();
		}
		// On ne lit PAS data[status] : PaymentService redemande le statut à l'API avec nos clés privées
		return new WebhookNotification(token);
	}

	private static <T> T call(java.util.function.Supplier<T> request) {
		try {
			return request.get();
		}
		catch (RestClientException ex) {
			throw new PaymentProviderException("PayDunya injoignable : " + ex.getMessage(), ex);
		}
	}

	static String sha512Hex(String value) {
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-512").digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-512 indisponible", ex);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record CreateInvoiceResponse(@JsonProperty("response_code") String responseCode,
			@JsonProperty("response_text") String responseText, String description, String token) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ConfirmResponse(@JsonProperty("response_code") String responseCode, String status, Invoice invoice) {

		@JsonIgnoreProperties(ignoreUnknown = true)
		record Invoice(@JsonProperty("total_amount") BigDecimal totalAmount, String token) {
		}

	}

}
