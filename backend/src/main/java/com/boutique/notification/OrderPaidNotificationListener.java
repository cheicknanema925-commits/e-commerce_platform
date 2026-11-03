package com.boutique.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.boutique.order.OrderPaidEvent;

// Écoute « commande payée ». Le module commande ne connaît pas cette classe : on peut en ajouter d'autres
// (statistiques, préparation en entrepôt…) sans modifier OrderService.
@Component
class OrderPaidNotificationListener {

	private final NotificationSender notificationSender;

	OrderPaidNotificationListener(NotificationSender notificationSender) {
		this.notificationSender = notificationSender;
	}

	// AFTER_COMMIT : exécuté seulement si la transaction du paiement est réellement enregistrée.
	// Si elle échoue (rollback), aucun message « paiement confirmé » n'est envoyé à tort.
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onOrderPaid(OrderPaidEvent event) {
		notificationSender.send(event.customerEmail(), "Paiement confirmé – commande n° " + event.orderId(),
				"Nous avons bien reçu votre paiement de " + event.totalAmount() + " FCFA. Votre commande est en préparation.");
	}

}
