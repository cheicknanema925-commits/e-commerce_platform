package com.boutique.preorder;

import com.boutique.common.BadRequestException;

public class InvalidPreOrderException extends BadRequestException {

	public InvalidPreOrderException() {
		super("La quantité doit être comprise entre 1 et 99.");
	}

}
