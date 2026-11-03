package com.boutique.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

// Le tableau des transitions, testé ligne par ligne : chaque ligne du CsvSource est un cas de test
class OrderStatusTest {

	@ParameterizedTest(name = "{0} → {1} par {2} : autorisé")
	@CsvSource({
		"CREATED,   PAID,      PAYMENT_PROVIDER",
		"CREATED,   CANCELLED, CUSTOMER",
		"CREATED,   CANCELLED, ADMIN",
		"PAID,      PREPARING, ADMIN",
		"PAID,      CANCELLED, ADMIN",
		"PREPARING, SHIPPED,   ADMIN",
		"SHIPPED,   DELIVERED, ADMIN" })
	void allowedTransitions(OrderStatus from, OrderStatus to, OrderActor actor) {
		assertThat(from.canTransitionTo(to, actor)).isTrue();
	}

	@ParameterizedTest(name = "{0} → {1} par {2} : refusé")
	@CsvSource({
		// Un client ne peut pas déclarer sa commande payée, l'admin non plus : seul le paiement confirmé fait foi
		"CREATED,   PAID,      CUSTOMER",
		"CREATED,   PAID,      ADMIN",
		// Une commande payée ne s'annule que par l'admin (il faudra rembourser)
		"PAID,      CANCELLED, CUSTOMER",
		// On ne saute pas d'étape et on ne revient pas en arrière
		"PAID,      DELIVERED, ADMIN",
		"SHIPPED,   PREPARING, ADMIN",
		// États finaux
		"DELIVERED, CANCELLED, ADMIN",
		"CANCELLED, PAID,      PAYMENT_PROVIDER" })
	void forbiddenTransitions(OrderStatus from, OrderStatus to, OrderActor actor) {
		assertThat(from.canTransitionTo(to, actor)).isFalse();
	}

	@Test
	void deliveredAndCancelledAreFinal() {
		for (OrderStatus target : OrderStatus.values()) {
			for (OrderActor actor : OrderActor.values()) {
				assertThat(OrderStatus.DELIVERED.canTransitionTo(target, actor)).isFalse();
				assertThat(OrderStatus.CANCELLED.canTransitionTo(target, actor)).isFalse();
			}
		}
	}

}
