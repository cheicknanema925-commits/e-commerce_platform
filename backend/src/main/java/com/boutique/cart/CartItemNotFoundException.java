package com.boutique.cart;

import com.boutique.common.NotFoundException;

public class CartItemNotFoundException extends NotFoundException {

	public CartItemNotFoundException(Long productId) {
		super("Ce produit n'est pas dans le panier : " + productId);
	}

}
