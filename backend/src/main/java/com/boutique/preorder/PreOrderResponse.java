package com.boutique.preorder;

import java.time.Instant;

public record PreOrderResponse(Long id, Long productId, String productName, int quantity, PreOrder.Status status,
		Instant createdAt, Instant notifiedAt) {

	static PreOrderResponse from(PreOrder preOrder) {
		return new PreOrderResponse(preOrder.getId(), preOrder.getProduct().getId(), preOrder.getProduct().getName(),
				preOrder.getQuantity(), preOrder.getStatus(), preOrder.getCreatedAt(), preOrder.getNotifiedAt());
	}

}
