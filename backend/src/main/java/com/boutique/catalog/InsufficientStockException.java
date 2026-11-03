package com.boutique.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientStockException extends RuntimeException {

	public InsufficientStockException(Long productId, int available, int requested) {
		super("Stock insuffisant pour le produit " + productId + " : " + available + " disponible(s), "
				+ requested + " demandé(s)");
	}

}
