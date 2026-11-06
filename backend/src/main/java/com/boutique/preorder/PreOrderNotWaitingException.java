package com.boutique.preorder;

import com.boutique.common.ConflictException;

public class PreOrderNotWaitingException extends ConflictException {

	public PreOrderNotWaitingException(Long id) {
		super("La pré-commande " + id + " n'est plus en attente.");
	}

}
