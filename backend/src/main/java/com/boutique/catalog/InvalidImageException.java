package com.boutique.catalog;

import com.boutique.common.BadRequestException;

public class InvalidImageException extends BadRequestException {

	public InvalidImageException(String message) {
		super(message);
	}

}
