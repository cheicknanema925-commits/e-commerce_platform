package com.boutique.catalog;

import com.boutique.common.ConflictException;

public class InsufficientStockException extends ConflictException {

	public InsufficientStockException(Long productId, int available, int requested) {
		super("Stock insuffisant pour le produit " + productId + " : " + available + " disponible(s), "
				+ requested + " demandé(s)");
	}

}
