package com.boutique.support;

import com.boutique.common.ConflictException;

public class TooManyOpenTicketsException extends ConflictException {

	public TooManyOpenTicketsException(int max) {
		super("Vous avez déjà " + max + " demandes en attente de réponse : le service client vous répond au plus vite.");
	}

}
