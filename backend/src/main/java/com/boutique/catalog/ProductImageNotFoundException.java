package com.boutique.catalog;

import com.boutique.common.NotFoundException;

public class ProductImageNotFoundException extends NotFoundException {

	public ProductImageNotFoundException(String reference) {
		super("Photo introuvable : " + reference);
	}

}
