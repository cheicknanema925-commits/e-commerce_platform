package com.boutique.preorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.boutique.catalog.ProductBackInStockEvent;

// Le catalogue et les commandes annoncent le retour en stock sans savoir que les pré-commandes existent
@Component
class BackInStockListener {

	private static final Logger log = LoggerFactory.getLogger(BackInStockListener.class);

	private final PreOrderService preOrderService;

	BackInStockListener(PreOrderService preOrderService) {
		this.preOrderService = preOrderService;
	}

	// AFTER_COMMIT : on ne prévient personne d'un réassort annulé. La transaction du réassort est terminée :
	// notifyBackInStock en ouvre une nouvelle pour marquer les demandes « notifiées »
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onBackInStock(ProductBackInStockEvent event) {
		int notified = preOrderService.notifyBackInStock(event.productId());
		if (notified > 0) {
			log.info("Produit {} de retour en stock : {} client(s) prévenu(s)", event.productId(), notified);
		}
	}

}
