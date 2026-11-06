package com.boutique.agent.client;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.boutique.agent.AgentKind;
import com.boutique.agent.StepHandler;
import com.boutique.cart.CartItemRequest;
import com.boutique.cart.CartService;

// Exécution d'un ajout au panier CONFIRMÉ : passe par CartService (stock vérifié, 99 maximum par produit)
@Component
class AddToCartHandler implements StepHandler {

	static final String TYPE = "ADD_TO_CART";

	private final CartService cartService;

	AddToCartHandler(CartService cartService) {
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
		cartService.addItem(userId, new CartItemRequest(productId, quantity));
		return "ajouté au panier";
	}

}
