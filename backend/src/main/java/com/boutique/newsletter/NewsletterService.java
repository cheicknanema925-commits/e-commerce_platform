package com.boutique.newsletter;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.notification.NotificationSender;

@Service
@Transactional(readOnly = true)
public class NewsletterService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final NewsletterSubscriberRepository repository;

	private final NotificationSender notificationSender;

	private final String frontendUrl;

	public NewsletterService(NewsletterSubscriberRepository repository, NotificationSender notificationSender,
			@Value("${boutique.payment.frontend-url}") String frontendUrl) {
		this.repository = repository;
		this.notificationSender = notificationSender;
		this.frontendUrl = frontendUrl;
	}

	// Même résultat que l'adresse soit nouvelle ou déjà inscrite : la réponse ne révèle pas qui est abonné.
	// Le message de bienvenue (avec le lien de désinscription) n'est envoyé qu'à une nouvelle inscription.
	@Transactional
	public void subscribe(String email) {
		String normalized = email.strip().toLowerCase(Locale.ROOT);
		String token = newToken();
		if (repository.insertIfAbsent(normalized, token) == 1) {
			notificationSender.send(normalized, "Bienvenue dans l'infolettre Shop Africa",
					"Vous recevrez nos promotions chaque semaine. Pour vous désinscrire : " + frontendUrl
							+ "/desinscription?jeton=" + token);
		}
	}

	// Jeton inconnu ou déjà utilisé : même réponse (rien à révéler)
	@Transactional
	public void unsubscribe(String token) {
		repository.deleteByUnsubscribeToken(token);
	}

	public Page<NewsletterSubscriber> subscribers(Pageable pageable) {
		return repository.findAll(pageable);
	}

	// 32 octets aléatoires (256 bits) en Base64 URL : impossible à deviner, utilisable dans un lien
	private static String newToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
