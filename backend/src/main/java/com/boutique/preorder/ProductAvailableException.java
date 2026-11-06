package com.boutique.preorder;

import com.boutique.common.ConflictException;

public class ProductAvailableException extends ConflictException {

	public ProductAvailableException(String productName) {
		super(productName + " est disponible : ajoutez-le directement au panier.");
	}

}
