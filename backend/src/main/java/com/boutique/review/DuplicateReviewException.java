package com.boutique.review;

import com.boutique.common.ConflictException;

public class DuplicateReviewException extends ConflictException {

	public DuplicateReviewException() {
		super("Vous avez déjà noté ce produit.");
	}

}
