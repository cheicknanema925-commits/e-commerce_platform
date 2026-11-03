package com.boutique.cart;

import com.boutique.common.BadRequestException;

public class CartQuantityTooLargeException extends BadRequestException {

	public CartQuantityTooLargeException(int max) {
		super("Quantité maximale par produit : " + max + ".");
	}

}
