package com.boutique.support;

import com.boutique.common.BadRequestException;

public class InvalidTicketException extends BadRequestException {

	public InvalidTicketException(String message) {
		super(message);
	}

}
