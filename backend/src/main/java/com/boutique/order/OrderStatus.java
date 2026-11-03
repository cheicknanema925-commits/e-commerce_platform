package com.boutique.order;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.boutique.order.OrderActor.ADMIN;
import static com.boutique.order.OrderActor.CUSTOMER;
import static com.boutique.order.OrderActor.PAYMENT_PROVIDER;
import static com.boutique.order.PaymentMethod.CASH_ON_DELIVERY;
import static com.boutique.order.PaymentMethod.ONLINE;

// Machine à états : chaque règle dit « de quel statut vers quel statut, par qui, pour quel mode de paiement ».
//
// Paiement en ligne :
//   CREATED ──(paiement)──► PAID ──(admin)──► PREPARING ──(admin)──► SHIPPED ──(admin)──► DELIVERED
//      └─(client, admin)─► CANCELLED ◄─(admin, remboursement)─┘
//
// Paiement à la livraison (espèces encaissées à la remise du colis) :
//   CREATED ──(admin confirme)──► PREPARING ──► SHIPPED ──(espèces reçues)──► DELIVERED
//      └─(client, admin)─► CANCELLED ◄─(colis refusé à la porte)─┘
public enum OrderStatus {

	CREATED, PAID, PREPARING, SHIPPED, DELIVERED, CANCELLED;

	private static final Set<PaymentMethod> ALL_METHODS = EnumSet.allOf(PaymentMethod.class);

	private static final List<Transition> TRANSITIONS = List.of(
			new Transition(CREATED, PAID, Set.of(PAYMENT_PROVIDER), Set.of(ONLINE)),
			new Transition(PAID, PREPARING, Set.of(ADMIN), Set.of(ONLINE)),
			new Transition(PAID, CANCELLED, Set.of(ADMIN), Set.of(ONLINE)),
			new Transition(CREATED, PREPARING, Set.of(ADMIN), Set.of(CASH_ON_DELIVERY)),
			new Transition(SHIPPED, CANCELLED, Set.of(ADMIN), Set.of(CASH_ON_DELIVERY)),
			new Transition(CREATED, CANCELLED, Set.of(CUSTOMER, ADMIN), ALL_METHODS),
			new Transition(PREPARING, SHIPPED, Set.of(ADMIN), ALL_METHODS),
			new Transition(SHIPPED, DELIVERED, Set.of(ADMIN), ALL_METHODS));

	public boolean canTransitionTo(OrderStatus target, OrderActor actor, PaymentMethod method) {
		return TRANSITIONS.stream()
			.anyMatch((t) -> t.from() == this && t.to() == target && t.actors().contains(actor)
					&& t.methods().contains(method));
	}

	private record Transition(OrderStatus from, OrderStatus to, Set<OrderActor> actors, Set<PaymentMethod> methods) {
	}

}
