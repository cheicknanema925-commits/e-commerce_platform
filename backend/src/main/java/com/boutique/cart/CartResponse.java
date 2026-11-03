package com.boutique.cart;

import java.math.BigDecimal;
import java.util.List;

// Prix ACTUELS du catalogue : le panier n'est pas un engagement, le prix est figé seulement à la commande
public record CartResponse(List<CartItemResponse> items, BigDecimal total) {

	static CartResponse empty() {
		return new CartResponse(List.of(), BigDecimal.ZERO);
	}

	static CartResponse from(Cart cart) {
		List<CartItemResponse> items = cart.getItems().stream().map(CartItemResponse::from).toList();
		BigDecimal total = items.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new CartResponse(items, total);
	}

	public record CartItemResponse(Long productId, String productName, BigDecimal unitPrice, int quantity,
			BigDecimal subtotal) {

		static CartItemResponse from(CartItem item) {
			BigDecimal price = item.getProduct().getPrice();
			return new CartItemResponse(item.getProduct().getId(), item.getProduct().getName(), price,
					item.getQuantity(), price.multiply(BigDecimal.valueOf(item.getQuantity())));
		}

	}

}
