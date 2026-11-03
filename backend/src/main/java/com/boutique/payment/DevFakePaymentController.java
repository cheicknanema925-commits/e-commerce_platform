package com.boutique.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.boutique.payment.PaymentProvider.ProviderStatus;

// Profil dev uniquement : simule la page de paiement du faux fournisseur, puis l'envoi de son webhook signé.
// POST /api/dev/fake-payments/{référence}/paye   ou   /annule
@RestController
@Profile("dev")
@ConditionalOnProperty(name = "boutique.payment.provider", havingValue = "fake", matchIfMissing = true)
@RequestMapping("/api/dev/fake-payments")
class DevFakePaymentController {

	private final FakePaymentProvider fakePaymentProvider;

	private final PaymentService paymentService;

	DevFakePaymentController(FakePaymentProvider fakePaymentProvider, PaymentService paymentService) {
		this.fakePaymentProvider = fakePaymentProvider;
		this.paymentService = paymentService;
	}

	@PostMapping("/{reference}/{outcome}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void simulate(@PathVariable String reference, @PathVariable String outcome) {
		ProviderStatus status = switch (outcome) {
			case "paye" -> ProviderStatus.SUCCEEDED;
			case "annule" -> ProviderStatus.FAILED;
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Résultat attendu : paye ou annule");
		};
		fakePaymentProvider.simulate(reference, status);
		paymentService.handleWebhook(fakePaymentProvider.name(), fakePaymentProvider.signedWebhook(reference));
	}

}
