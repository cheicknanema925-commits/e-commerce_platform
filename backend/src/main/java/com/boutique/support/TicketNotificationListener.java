package com.boutique.support;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.boutique.notification.NotificationSender;

@Component
class TicketNotificationListener {

	private final NotificationSender notificationSender;

	TicketNotificationListener(NotificationSender notificationSender) {
		this.notificationSender = notificationSender;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onTicketAnswered(TicketAnsweredEvent event) {
		notificationSender.send(event.customerEmail(), "Réponse à votre demande « " + event.subject() + " »",
				"Le service client Shop Africa a répondu à votre demande n° " + event.ticketId()
						+ ". Retrouvez la réponse dans votre espace « Mes demandes ».");
	}

}
