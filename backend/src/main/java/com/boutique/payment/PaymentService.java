package com.boutique.payment;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.boutique.order.Order;
import com.boutique.order.OrderDeliveredEvent;
import com.boutique.order.OrderNotFoundException;
import com.boutique.order.OrderRepository;
import com.boutique.order.OrderService;
import com.boutique.order.OrderStatus;
import com.boutique.order.PaymentMethod;
import com.boutique.payment.PaymentExceptions.InvalidWebhookException;
import com.boutique.payment.PaymentExceptions.PaymentNotAllowedException;
import com.boutique.payment.PaymentExceptions.PaymentNotFoundException;
import com.boutique.payment.PaymentExceptions.UnknownProviderException;
import com.boutique.payment.PaymentProvider.PaymentRequest;
import com.boutique.payment.PaymentProvider.PaymentSession;
import com.boutique.payment.PaymentProvider.VerifiedPayment;
import com.boutique.payment.PaymentProvider.WebhookNotification;
import com.boutique.payment.PaymentProvider.WebhookRequest;

// Règle d'or : le SERVEUR décide de tout. Le montant vient de la commande en base, le statut « payé » vient
// seulement d'une vérification auprès du fournisseur, jamais de la page de retour ni du contenu brut d'un webhook.
@Service
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	// Fournisseur des paiements en espèces enregistrés à la livraison
	public static final String CASH = "cash";

	private final PaymentProvider provider;

	private final PaymentRepository paymentRepository;

	private final PaymentEventRepository eventRepository;

	private final OrderRepository orderRepository;

	private final OrderService orderService;

	private final PaymentProperties properties;

	private final TransactionTemplate transactionTemplate;

	public PaymentService(PaymentProvider provider, PaymentRepository paymentRepository,
			PaymentEventRepository eventRepository, OrderRepository orderRepository, OrderService orderService,
			PaymentProperties properties, TransactionTemplate transactionTemplate) {
		this.provider = provider;
		this.paymentRepository = paymentRepository;
		this.eventRepository = eventRepository;
		this.orderRepository = orderRepository;
		this.orderService = orderService;
		this.properties = properties;
		this.transactionTemplate = transactionTemplate;
	}

	// Volontairement SANS @Transactional : on ne garde jamais une transaction (et une connexion à la base)
	// ouverte pendant un appel réseau vers le fournisseur. 1) lecture  2) appel au fournisseur  3) écriture
	public PaymentResponse initiate(Long userId, Long orderId) {
		PaymentRequest request = transactionTemplate.execute((status) -> {
			Order order = orderRepository.findByIdAndUserId(orderId, userId)
				.orElseThrow(() -> new OrderNotFoundException(orderId));
			if (order.getPaymentMethod() != PaymentMethod.ONLINE || order.getStatus() != OrderStatus.CREATED) {
				throw new PaymentNotAllowedException();
			}
			String orderPage = properties.frontendUrl() + "/commandes/" + orderId;
			return new PaymentRequest(orderId, order.getTotalAmount(), "Commande n° " + orderId + " – " + properties.storeName(),
					orderPage + "?paiement=retour", orderPage + "?paiement=annule",
					properties.publicApiUrl() + "/api/payments/webhooks/" + provider.name());
		});
		PaymentSession session = provider.initiate(request);
		return transactionTemplate.execute((status) -> {
			Payment payment = paymentRepository.save(new Payment(orderRepository.getReferenceById(orderId),
					provider.name(), request.amount(), session.reference()));
			record(payment.getId(), PaymentEventType.INITIATED, "référence " + session.reference());
			return new PaymentResponse(payment.getId(), payment.getStatus(), payment.getAmount(), session.checkoutUrl());
		});
	}

	// noRollbackFor : un webhook rejeté doit rester dans le journal même si on répond par une erreur
	@Transactional(noRollbackFor = InvalidWebhookException.class)
	public void handleWebhook(String providerName, WebhookRequest request) {
		if (!provider.name().equals(providerName)) {
			throw new UnknownProviderException(providerName);
		}
		WebhookNotification notification;
		try {
			notification = provider.parseWebhook(request);
		}
		catch (InvalidWebhookException ex) {
			record(null, PaymentEventType.WEBHOOK_REJECTED, "signature invalide");
			log.warn("Webhook {} rejeté : signature invalide", providerName);
			throw ex;
		}
		Payment payment = paymentRepository.findByProviderReference(notification.reference())
			.orElseThrow(() -> new PaymentNotFoundException(notification.reference()));
		record(payment.getId(), PaymentEventType.WEBHOOK_RECEIVED, null);

		// Idempotence : un webhook reçu deux fois (le fournisseur réessaie souvent) ne paie pas deux fois
		if (!payment.isPending()) {
			record(payment.getId(), PaymentEventType.DUPLICATE_IGNORED, "déjà " + payment.getStatus());
			return;
		}
		// On ne croit jamais le contenu du webhook : on redemande statut et montant au fournisseur
		apply(payment, provider.verify(notification.reference()));
	}

	// Réconciliation : pour un paiement resté PENDING (webhook perdu, serveur arrêté…), on redemande son statut
	@Transactional
	public void reconcile(Long paymentId) {
		Payment payment = paymentRepository.findById(paymentId).orElseThrow();
		if (payment.isPending()) {
			record(payment.getId(), PaymentEventType.RECONCILED, null);
			apply(payment, provider.verify(payment.getProviderReference()));
		}
	}

	@Transactional(readOnly = true)
	public List<Long> findPendingPaymentIds(Duration olderThan) {
		return paymentRepository.findPendingIds(provider.name(), Instant.now().minus(olderThan));
	}

	// Paiement à la livraison : écouté de façon SYNCHRONE, dans la transaction qui passe la commande à DELIVERED.
	// Les espèces encaissées par le livreur sont enregistrées comme un paiement réussi.
	@EventListener
	public void onOrderDelivered(OrderDeliveredEvent event) {
		if (event.paymentMethod() != PaymentMethod.CASH_ON_DELIVERY) {
			return;
		}
		Payment payment = new Payment(orderRepository.getReferenceById(event.orderId()), CASH, event.totalAmount(),
				null);
		payment.succeed();
		paymentRepository.save(payment);
		eventRepository.save(new PaymentEvent(payment.getId(), CASH, PaymentEventType.CASH_COLLECTED,
				event.totalAmount() + " FCFA encaissés à la livraison"));
	}

	private void apply(Payment payment, VerifiedPayment verified) {
		switch (verified.status()) {
			case PENDING -> {
				// Pas encore décidé chez le fournisseur : la réconciliation repassera plus tard
			}
			case FAILED -> {
				payment.fail();
				record(payment.getId(), PaymentEventType.FAILED, null);
			}
			case SUCCEEDED -> {
				if (verified.amount() == null || verified.amount().compareTo(payment.getAmount()) != 0) {
					// Montant payé différent du montant de la commande : on ne valide surtout pas
					payment.fail();
					record(payment.getId(), PaymentEventType.AMOUNT_MISMATCH,
							"attendu " + payment.getAmount() + ", reçu " + verified.amount());
					log.warn("Paiement {} : montant incohérent (attendu {}, reçu {})", payment.getId(),
							payment.getAmount(), verified.amount());
					return;
				}
				payment.succeed();
				record(payment.getId(), PaymentEventType.SUCCEEDED, null);
				if (payment.getOrder().getStatus() == OrderStatus.CREATED) {
					orderService.markPaid(payment.getOrder().getId());
				}
				else {
					// Payée APRÈS une annulation (le client a annulé pendant qu'il payait) : à rembourser
					record(payment.getId(), PaymentEventType.REFUND_REQUIRED,
							"commande " + payment.getOrder().getStatus());
				}
			}
		}
	}

	private void record(Long paymentId, PaymentEventType type, String detail) {
		eventRepository.save(new PaymentEvent(paymentId, provider.name(), type, detail));
	}

}
