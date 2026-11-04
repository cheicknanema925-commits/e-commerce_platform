package com.boutique.catalog;

import com.boutique.common.BadRequestException;

public class InvalidPromotionException extends BadRequestException {

	public InvalidPromotionException() {
		super("Le prix barré doit être supérieur au prix de vente.");
	}

}
