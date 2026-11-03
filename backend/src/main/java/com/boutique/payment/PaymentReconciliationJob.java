package com.boutique.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Filet de sécurité : un webhook peut se perdre (panne réseau, serveur redémarré). Toutes les N minutes, on
// redemande au fournisseur le statut des paiements restés en attente. Chaque paiement a sa propre transaction.
@Component
public class PaymentReconciliationJob {

	private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationJob.class);

	private final PaymentService paymentService;

	private final PaymentProperties properties;

	public PaymentReconciliationJob(PaymentService paymentService, PaymentProperties properties) {
		this.paymentService = paymentService;
		this.properties = properties;
	}

	@Scheduled(fixedDelayString = "${boutique.payment.reconciliation.interval}",
			initialDelayString = "${boutique.payment.reconciliation.interval}")
	public void reconcilePendingPayments() {
		for (Long paymentId : paymentService.findPendingPaymentIds(properties.reconciliation().pendingDelay())) {
			try {
				paymentService.reconcile(paymentId);
			}
			catch (RuntimeException ex) {
				// Un échec (fournisseur injoignable…) ne doit pas empêcher de traiter les autres paiements
				log.warn("Réconciliation impossible pour le paiement {} : {}", paymentId, ex.getMessage());
			}
		}
	}

}
