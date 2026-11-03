package com.boutique.review;

import com.boutique.common.ForbiddenException;

public class ReviewNotAllowedException extends ForbiddenException {

	public ReviewNotAllowedException() {
		super("Vous ne pouvez noter qu'un produit que vous avez acheté et reçu.");
	}

}
