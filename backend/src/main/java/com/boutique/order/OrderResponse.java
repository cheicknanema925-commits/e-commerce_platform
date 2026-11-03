package com.boutique.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, OrderStatus status, BigDecimal totalAmount, Instant createdAt,
		List<OrderItemResponse> items) {

	static OrderResponse from(Order order) {
		return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount(), order.getCreatedAt(),
				order.getItems().stream().map(OrderItemResponse::from).toList());
	}

}
