package com.boutique.catalog;

import com.boutique.common.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {

	public CategoryNotFoundException(Long id) {
		super("Catégorie introuvable : " + id);
	}

}
