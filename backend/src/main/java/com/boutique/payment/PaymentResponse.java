package com.boutique.payment;

import java.math.BigDecimal;

// checkoutUrl : la page de paiement HÉBERGÉE par le fournisseur, vers laquelle le frontend redirige le client.
// L'API ne voit jamais de numéro de carte ni de code Wave / Orange Money.
public record PaymentResponse(Long paymentId, PaymentStatus status, BigDecimal amount, String checkoutUrl) {

}
