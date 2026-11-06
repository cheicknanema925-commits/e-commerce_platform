package com.boutique.pricing;

import com.boutique.common.BadRequestException;

public class InvalidPriceChangeException extends BadRequestException {

	public InvalidPriceChangeException(String message) {
		super(message);
	}

}
