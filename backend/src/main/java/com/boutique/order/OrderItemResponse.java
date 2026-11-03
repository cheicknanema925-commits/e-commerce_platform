package com.boutique.order;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId, String productName, BigDecimal unitPrice, int quantity,
		BigDecimal subtotal) {

	static OrderItemResponse from(OrderItem item) {
		return new OrderItemResponse(item.getProduct().getId(), item.getProductName(), item.getUnitPrice(),
				item.getQuantity(), item.getSubtotal());
	}

}
