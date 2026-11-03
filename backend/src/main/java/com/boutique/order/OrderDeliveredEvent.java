package com.boutique.order;

import java.math.BigDecimal;

// Publié quand une commande est remise au client. Le module paiement l'écoute pour enregistrer les espèces encaissées
// (paiement à la livraison) : la commande n'a pas besoin de connaître le module paiement.
public record OrderDeliveredEvent(Long orderId, PaymentMethod paymentMethod, BigDecimal totalAmount) {

}
