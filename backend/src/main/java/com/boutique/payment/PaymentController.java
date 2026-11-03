package com.boutique.payment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.payment.PaymentProvider.WebhookRequest;
import com.boutique.user.CurrentUserResolver;

@RestController
public class PaymentController {

	private final PaymentService paymentService;

	private final CurrentUserResolver currentUserResolver;

	public PaymentController(PaymentService paymentService, CurrentUserResolver currentUserResolver) {
		this.paymentService = paymentService;
		this.currentUserResolver = currentUserResolver;
	}

	// Le client demande à payer SA commande : aucun montant dans la requête, il est relu en base
	@PostMapping("/api/orders/{orderId}/payment")
	public PaymentResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable Long orderId) {
		return paymentService.initiate(currentUserResolver.userId(jwt), orderId);
	}

	// Appelé par le FOURNISSEUR (serveur à serveur), sans jeton : l'authenticité est vérifiée par le fournisseur
	// lui-même (signature), puis le statut est reconfirmé auprès de son API
	@PostMapping("/api/payments/webhooks/{provider}")
	public ResponseEntity<Void> webhook(@PathVariable String provider, HttpServletRequest request) {
		paymentService.handleWebhook(provider, toWebhookRequest(request));
		return ResponseEntity.ok().build();
	}

	private static WebhookRequest toWebhookRequest(HttpServletRequest request) {
		Map<String, String> parameters = new HashMap<>();
		request.getParameterMap().forEach((name, values) -> parameters.put(name, (values.length > 0) ? values[0] : null));
		Map<String, String> headers = new HashMap<>();
		Collections.list(request.getHeaderNames())
			.forEach((name) -> headers.put(name.toLowerCase(Locale.ROOT), request.getHeader(name)));
		return new WebhookRequest(parameters, headers);
	}

}
