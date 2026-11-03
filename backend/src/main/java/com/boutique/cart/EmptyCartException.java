package com.boutique.cart;

import com.boutique.common.BadRequestException;

public class EmptyCartException extends BadRequestException {

	public EmptyCartException() {
		super("Le panier est vide.");
	}

}
