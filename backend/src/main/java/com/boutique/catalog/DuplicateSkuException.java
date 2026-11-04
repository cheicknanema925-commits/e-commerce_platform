package com.boutique.catalog;

import com.boutique.common.ConflictException;

public class DuplicateSkuException extends ConflictException {

	public DuplicateSkuException(String sku) {
		super("La référence " + sku + " est déjà utilisée par un autre produit.");
	}

}
