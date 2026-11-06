package com.boutique.pricing;

import com.boutique.common.ConflictException;

public class PricesChangedException extends ConflictException {

	public PricesChangedException(String productName) {
		super("Le prix de « " + productName + " » a changé depuis l'aperçu : rien n'a été modifié, refaites l'aperçu.");
	}

}
