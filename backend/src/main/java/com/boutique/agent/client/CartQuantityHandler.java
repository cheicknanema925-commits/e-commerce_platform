package com.boutique.agent.client;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.boutique.agent.AgentKind;
import com.boutique.agent.StepHandler;
import com.boutique.cart.CartService;

// Quantité 0 = retirer l'article ; sinon le panier revérifie le stock et le maximum de 99
@Component
class CartQuantityHandler implements StepHandler {

	static final String TYPE = "SET_CART_QUANTITY";

	private final CartService cartService;

	CartQuantityHandler(CartService cartService) {
		this.cartService = cartService;
	}

	@Override
	public String type() {
		return TYPE;
	}

	@Override
	public AgentKind agent() {
		return AgentKind.CLIENT;
	}

	@Override
	public String execute(Long userId, Map<String, Object> params) {
		long productId = StepHandler.longParam(params, "productId");
		int quantity = (int) StepHandler.longParam(params, "quantity");
		if (quantity == 0) {
			cartService.removeItem(userId, productId);
			return "retiré du panier";
		}
		cartService.changeQuantity(userId, productId, quantity);
		return "quantité mise à jour";
	}

}
