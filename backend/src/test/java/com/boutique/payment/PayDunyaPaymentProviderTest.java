package com.boutique.payment;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.boutique.common.ExternalServiceException;
import com.boutique.payment.PaymentExceptions.InvalidWebhookException;
import com.boutique.payment.PaymentProvider.PaymentRequest;
import com.boutique.payment.PaymentProvider.PaymentSession;
import com.boutique.payment.PaymentProvider.ProviderStatus;
import com.boutique.payment.PaymentProvider.VerifiedPayment;
import com.boutique.payment.PaymentProvider.WebhookRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// L'intégration PayDunya testée contre un FAUX serveur HTTP (MockRestServiceServer) qui renvoie les réponses
// décrites dans la documentation officielle : aucun appel réel, aucune clé réelle
class PayDunyaPaymentProviderTest {

	private static final PaymentProperties.PayDunya CONFIG = new PaymentProperties.PayDunya("test",
			"cle-principale-de-test", "test_private_cle", "jeton-de-test");

	private static final String SANDBOX = "https://app.paydunya.com/sandbox-api/v1";

	private MockRestServiceServer server;

	private PayDunyaPaymentProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		provider = new PayDunyaPaymentProvider(builder, CONFIG, "Boutique");
	}

	@Test
	void initiateCreatesASandboxInvoiceWithTheServerAmountAndOurKeys() {
		server.expect(requestTo(SANDBOX + "/checkout-invoice/create"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("PAYDUNYA-MASTER-KEY", "cle-principale-de-test"))
			.andExpect(header("PAYDUNYA-PRIVATE-KEY", "test_private_cle"))
			.andExpect(header("PAYDUNYA-TOKEN", "jeton-de-test"))
			.andExpect(jsonPath("$.invoice.total_amount").value(50000))
			.andExpect(jsonPath("$.store.name").value("Boutique"))
			.andExpect(jsonPath("$.actions.callback_url").value("https://api.boutique.test/api/payments/webhooks/paydunya"))
			.andExpect(jsonPath("$.custom_data.order_id").value(42))
			.andRespond(withSuccess("""
					{"response_code": "00", "response_text": "https://app.paydunya.com/sandbox-checkout/invoice/test_Ab12",
					 "description": "Checkout Invoice Created", "token": "test_Ab12"}
					""", MediaType.APPLICATION_JSON));

		PaymentSession session = provider.initiate(request(new BigDecimal("50000.00")));

		assertThat(session.reference()).isEqualTo("test_Ab12");
		assertThat(session.checkoutUrl()).isEqualTo("https://app.paydunya.com/sandbox-checkout/invoice/test_Ab12");
		server.verify();
	}

	@Test
	void refusedInvoiceBecomesAProviderError() {
		server.expect(requestTo(SANDBOX + "/checkout-invoice/create"))
			.andRespond(withSuccess("{\"response_code\": \"1001\", \"description\": \"Invalid Masterkey\"}",
					MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> provider.initiate(request(new BigDecimal("50000")))).isInstanceOf(ExternalServiceException.class);
	}

	@Test
	void unavailableProviderBecomesAProviderError() {
		server.expect(requestTo(SANDBOX + "/checkout-invoice/create")).andRespond(withServerError());

		assertThatThrownBy(() -> provider.initiate(request(new BigDecimal("50000")))).isInstanceOf(ExternalServiceException.class);
	}

	// Le franc CFA n'a pas de centimes : on refuse plutôt que d'arrondir un montant en silence
	@Test
	void amountWithCentsIsRefused() {
		assertThatThrownBy(() -> provider.initiate(request(new BigDecimal("50000.50")))).isInstanceOf(ArithmeticException.class);
	}

	@Test
	void verifyReadsTheRealStatusAndAmount() {
		server.expect(requestTo(SANDBOX + "/checkout-invoice/confirm/test_Ab12"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("PAYDUNYA-PRIVATE-KEY", "test_private_cle"))
			.andRespond(withSuccess("""
					{"response_code": "00", "response_text": "Transaction Found", "status": "completed", "mode": "test",
					 "invoice": {"token": "test_Ab12", "total_amount": "50000", "description": "Commande n° 42"}}
					""", MediaType.APPLICATION_JSON));
		server.expect(requestTo(SANDBOX + "/checkout-invoice/confirm/test_Cd34"))
			.andRespond(withSuccess("""
					{"response_code": "00", "status": "cancelled", "invoice": {"token": "test_Cd34", "total_amount": 50000}}
					""", MediaType.APPLICATION_JSON));

		VerifiedPayment completed = provider.verify("test_Ab12");
		VerifiedPayment cancelled = provider.verify("test_Cd34");

		assertThat(completed.status()).isEqualTo(ProviderStatus.SUCCEEDED);
		assertThat(completed.amount()).isEqualByComparingTo("50000");
		assertThat(cancelled.status()).isEqualTo(ProviderStatus.FAILED);
	}

	// IPN : data[hash] doit être le SHA-512 de la MasterKey (documentation PayDunya)
	@Test
	void webhookWithTheMasterKeyHashIsAccepted() {
		WebhookRequest ipn = new WebhookRequest(Map.of("data[hash]", PayDunyaPaymentProvider.sha512Hex("cle-principale-de-test"),
				"data[invoice][token]", "test_Ab12", "data[status]", "completed"), Map.of());

		assertThat(provider.parseWebhook(ipn).reference()).isEqualTo("test_Ab12");
	}

	@Test
	void webhookWithAnotherHashIsRejected() {
		WebhookRequest forged = new WebhookRequest(Map.of("data[hash]", PayDunyaPaymentProvider.sha512Hex("une-autre-cle"),
				"data[invoice][token]", "test_Ab12", "data[status]", "completed"), Map.of());

		assertThatThrownBy(() -> provider.parseWebhook(forged)).isInstanceOf(InvalidWebhookException.class);
	}

	@Test
	void missingKeysStopTheApplicationAtStartup() {
		PaymentProperties.PayDunya noKeys = new PaymentProperties.PayDunya("test", "", "", "");

		assertThatThrownBy(() -> new PayDunyaPaymentProvider(RestClient.builder(), noKeys, "Boutique"))
			.isInstanceOf(IllegalStateException.class);
	}

	private static PaymentRequest request(BigDecimal amount) {
		return new PaymentRequest(42L, amount, "Commande n° 42", "https://boutique.test/commandes/42?paiement=retour",
				"https://boutique.test/commandes/42?paiement=annule", "https://api.boutique.test/api/payments/webhooks/paydunya");
	}

}
