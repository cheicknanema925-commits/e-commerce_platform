package com.boutique.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

// Vue back-office d'une commande : le client, et les statuts que l'ADMIN peut choisir ensuite.
// nextStatuses est calculé par la machine à états du serveur : le frontend n'a pas à recopier les règles.
public record AdminOrderResponse(Long id, OrderStatus status, PaymentMethod paymentMethod, BigDecimal totalAmount,
		Instant createdAt, String customerName, String customerEmail, List<OrderItemResponse> items,
		List<OrderStatus> nextStatuses) {

	static AdminOrderResponse from(Order order) {
		List<OrderStatus> next = Arrays.stream(OrderStatus.values())
			.filter((target) -> order.getStatus().canTransitionTo(target, OrderActor.ADMIN, order.getPaymentMethod()))
			.toList();
		return new AdminOrderResponse(order.getId(), order.getStatus(), order.getPaymentMethod(), order.getTotalAmount(),
				order.getCreatedAt(), order.getUser().getFullName(), order.getUser().getEmail(),
				order.getItems().stream().map(OrderItemResponse::from).toList(), next);
	}

}
