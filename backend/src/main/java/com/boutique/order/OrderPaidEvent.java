package com.boutique.order;

import java.math.BigDecimal;

// Événement publié quand le paiement d'une commande est confirmé. Le module commande annonce « c'est payé » ;
// il ne sait pas qui écoute (notification, statistiques, préparation…) et n'a pas à le savoir.
public record OrderPaidEvent(Long orderId, String customerEmail, BigDecimal totalAmount) {

}
