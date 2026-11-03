package com.boutique.catalog;

import com.boutique.common.NotFoundException;

public class ProductNotFoundException extends NotFoundException {

	public ProductNotFoundException(Long id) {
		super("Produit introuvable : " + id);
	}

}
