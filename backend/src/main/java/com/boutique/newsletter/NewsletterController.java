package com.boutique.newsletter;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.common.PageResponse;

@RestController
public class NewsletterController {

	private final NewsletterService newsletterService;

	public NewsletterController(NewsletterService newsletterService) {
		this.newsletterService = newsletterService;
	}

	// Public, limité en débit (AuthRateLimitFilter). 202 « Accepted » : toujours la même réponse
	@PostMapping("/api/newsletter/subscriptions")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Map<String, String> subscribe(@Valid @RequestBody SubscriptionRequest request) {
		newsletterService.subscribe(request.email());
		return Map.of("message", "Merci ! Vous recevrez nos promotions de la semaine.");
	}

	// POST et non un simple lien GET : les logiciels qui ouvrent les liens des emails (antivirus, aperçus) ne
	// désinscrivent personne par accident ; la page du site affiche un bouton qui envoie ce POST
	@PostMapping("/api/newsletter/unsubscribe")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsubscribe(@Valid @RequestBody UnsubscribeRequest request) {
		newsletterService.unsubscribe(request.token());
	}

	@GetMapping("/api/admin/newsletter")
	@PreAuthorize("hasRole('ADMIN')")
	public PageResponse<SubscriberResponse> subscribers(
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return PageResponse.from(newsletterService.subscribers(pageable).map(SubscriberResponse::from));
	}

	// Jamais le jeton de désinscription : seul l'abonné doit le connaître
	public record SubscriberResponse(Long id, String email, Instant createdAt) {

		static SubscriberResponse from(NewsletterSubscriber subscriber) {
			return new SubscriberResponse(subscriber.getId(), subscriber.getEmail(), subscriber.getCreatedAt());
		}

	}

}
