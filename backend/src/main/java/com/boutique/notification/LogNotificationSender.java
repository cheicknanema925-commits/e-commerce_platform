package com.boutique.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Implémentation de développement : le message est écrit dans les logs au lieu d'être envoyé
@Component
class LogNotificationSender implements NotificationSender {

	private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

	@Override
	public void send(String to, String subject, String body) {
		log.info("Notification à {} : {} — {}", to, subject, body);
	}

}
