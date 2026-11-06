package com.boutique.pricing;

import com.boutique.common.ConflictException;

public class NothingToUndoException extends ConflictException {

	public NothingToUndoException() {
		super("Aucun changement de prix à annuler.");
	}

}
