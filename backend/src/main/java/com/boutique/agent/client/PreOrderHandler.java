package com.boutique.agent.client;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.boutique.agent.AgentKind;
import com.boutique.agent.StepHandler;
import com.boutique.preorder.PreOrderService;

@Component
class PreOrderHandler implements StepHandler {

	static final String TYPE = "PRE_ORDER";

	private final PreOrderService preOrderService;

	PreOrderHandler(PreOrderService preOrderService) {
		this.preOrderService = preOrderService;
	}

	// Même libellé que l'outil passe par proposePreOrder ou par proposeAddToCart (produit en rupture)
	static String summary(String productName, int quantity) {
		return "Être prévenu du retour de " + productName + " (" + quantity + " souhaité" + (quantity > 1 ? "s" : "")
				+ ", rien n'est réservé)";
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
		preOrderService.preOrder(userId, StepHandler.longParam(params, "productId"),
				(int) StepHandler.longParam(params, "quantity"));
		return "vous serez prévenu dès son retour en stock";
	}

}
