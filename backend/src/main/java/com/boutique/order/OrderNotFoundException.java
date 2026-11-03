package com.boutique.order;

import com.boutique.common.NotFoundException;

public class OrderNotFoundException extends NotFoundException {

	public OrderNotFoundException(Long id) {
		super("Commande introuvable : " + id);
	}

}
