package com.boutique.order;

import com.boutique.common.NotFoundException;

public class DeliveryNotFoundException extends NotFoundException {

	public DeliveryNotFoundException(Long orderId) {
		super("Aucune livraison pour la commande " + orderId);
	}

}
