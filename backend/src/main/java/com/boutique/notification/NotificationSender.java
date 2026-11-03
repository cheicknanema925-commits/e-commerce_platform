package com.boutique.notification;

// Envoi d'un message au client. Aujourd'hui dans les logs ; demain un email, un SMS ou un WhatsApp, sans toucher au reste.
public interface NotificationSender {

	void send(String to, String subject, String body);

}
