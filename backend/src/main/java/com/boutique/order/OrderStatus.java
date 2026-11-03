package com.boutique.order;

import java.util.Map;
import java.util.Set;

import static com.boutique.order.OrderActor.ADMIN;
import static com.boutique.order.OrderActor.CUSTOMER;
import static com.boutique.order.OrderActor.PAYMENT_PROVIDER;

// Machine à états : chaque statut liste les statuts suivants possibles ET qui peut déclencher le passage.
//
//   CREATED ──(paiement)──► PAID ──(admin)──► PREPARING ──(admin)──► SHIPPED ──(admin)──► DELIVERED
//      │                     │
//      └─(client, admin)─► CANCELLED ◄─(admin)─┘
public enum OrderStatus {

	CREATED, PAID, PREPARING, SHIPPED, DELIVERED, CANCELLED;

	private static final Map<OrderStatus, Map<OrderStatus, Set<OrderActor>>> TRANSITIONS = Map.of(
			CREATED, Map.of(PAID, Set.of(PAYMENT_PROVIDER), CANCELLED, Set.of(CUSTOMER, ADMIN)),
			PAID, Map.of(PREPARING, Set.of(ADMIN), CANCELLED, Set.of(ADMIN)),
			PREPARING, Map.of(SHIPPED, Set.of(ADMIN)),
			SHIPPED, Map.of(DELIVERED, Set.of(ADMIN)),
			DELIVERED, Map.of(),
			CANCELLED, Map.of());

	public boolean canTransitionTo(OrderStatus target, OrderActor actor) {
		return TRANSITIONS.get(this).getOrDefault(target, Set.of()).contains(actor);
	}

}
