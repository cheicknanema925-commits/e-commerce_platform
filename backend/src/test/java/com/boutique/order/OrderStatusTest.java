package com.boutique.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

// Le tableau des transitions, testé ligne par ligne : chaque ligne du CsvSource est un cas de test
class OrderStatusTest {

	@ParameterizedTest(name = "{0} → {1} par {2} ({3}) : autorisé")
	@CsvSource({
		// Paiement en ligne
		"CREATED,   PAID,      PAYMENT_PROVIDER, ONLINE",
		"PAID,      PREPARING, ADMIN,            ONLINE",
		"PAID,      CANCELLED, ADMIN,            ONLINE",
		// Paiement à la livraison : préparée sans être payée, refusable à la porte
		"CREATED,   PREPARING, ADMIN,            CASH_ON_DELIVERY",
		"SHIPPED,   CANCELLED, ADMIN,            CASH_ON_DELIVERY",
		// Communes aux deux modes
		"CREATED,   CANCELLED, CUSTOMER,         ONLINE",
		"CREATED,   CANCELLED, ADMIN,            CASH_ON_DELIVERY",
		"PREPARING, SHIPPED,   ADMIN,            CASH_ON_DELIVERY",
		"SHIPPED,   DELIVERED, ADMIN,            ONLINE" })
	void allowedTransitions(OrderStatus from, OrderStatus to, OrderActor actor, PaymentMethod method) {
		assertThat(from.canTransitionTo(to, actor, method)).isTrue();
	}

	@ParameterizedTest(name = "{0} → {1} par {2} ({3}) : refusé")
	@CsvSource({
		// Seul le paiement confirmé fait foi : ni le client ni l'admin ne déclarent une commande payée
		"CREATED,   PAID,      CUSTOMER,         ONLINE",
		"CREATED,   PAID,      ADMIN,            ONLINE",
		// Une commande en espèces n'est jamais payée en ligne
		"CREATED,   PAID,      PAYMENT_PROVIDER, CASH_ON_DELIVERY",
		// En ligne : pas de préparation avant le paiement
		"CREATED,   PREPARING, ADMIN,            ONLINE",
		// Une commande payée ne s'annule que par l'admin (il faudra rembourser)
		"PAID,      CANCELLED, CUSTOMER,         ONLINE",
		// On ne saute pas d'étape et on ne revient pas en arrière
		"PAID,      DELIVERED, ADMIN,            ONLINE",
		"SHIPPED,   PREPARING, ADMIN,            ONLINE",
		// Le refus à la porte n'existe que pour le paiement à la livraison
		"SHIPPED,   CANCELLED, ADMIN,            ONLINE",
		// États finaux
		"DELIVERED, CANCELLED, ADMIN,            CASH_ON_DELIVERY",
		"CANCELLED, PAID,      PAYMENT_PROVIDER, ONLINE" })
	void forbiddenTransitions(OrderStatus from, OrderStatus to, OrderActor actor, PaymentMethod method) {
		assertThat(from.canTransitionTo(to, actor, method)).isFalse();
	}

	@Test
	void deliveredAndCancelledAreFinal() {
		for (OrderStatus target : OrderStatus.values()) {
			for (OrderActor actor : OrderActor.values()) {
				for (PaymentMethod method : PaymentMethod.values()) {
					assertThat(OrderStatus.DELIVERED.canTransitionTo(target, actor, method)).isFalse();
					assertThat(OrderStatus.CANCELLED.canTransitionTo(target, actor, method)).isFalse();
				}
			}
		}
	}

}
