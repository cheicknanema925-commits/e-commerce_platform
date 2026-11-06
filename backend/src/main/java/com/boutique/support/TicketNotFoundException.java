package com.boutique.support;

import com.boutique.common.NotFoundException;

public class TicketNotFoundException extends NotFoundException {

	public TicketNotFoundException(Long id) {
		super("Demande introuvable : " + id);
	}

}
