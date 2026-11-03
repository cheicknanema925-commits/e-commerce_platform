package com.boutique.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, OrderStatus status, PaymentMethod paymentMethod, BigDecimal totalAmount,
		Instant createdAt, List<OrderItemResponse> items) {

	static OrderResponse from(Order order) {
		return new OrderResponse(order.getId(), order.getStatus(), order.getPaymentMethod(), order.getTotalAmount(),
				order.getCreatedAt(), order.getItems().stream().map(OrderItemResponse::from).toList());
	}

}
