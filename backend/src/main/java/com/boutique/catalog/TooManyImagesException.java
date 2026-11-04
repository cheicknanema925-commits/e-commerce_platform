package com.boutique.catalog;

import com.boutique.common.ConflictException;

public class TooManyImagesException extends ConflictException {

	public TooManyImagesException(int max) {
		super("Un produit ne peut pas avoir plus de " + max + " photos. Supprimez-en une avant d'en ajouter.");
	}

}
