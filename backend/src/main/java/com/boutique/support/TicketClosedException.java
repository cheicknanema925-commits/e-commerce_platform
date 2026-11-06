package com.boutique.support;

import com.boutique.common.ConflictException;

public class TicketClosedException extends ConflictException {

	public TicketClosedException(Long id) {
		super("La demande " + id + " est close.");
	}

}
