package com.boutique.preorder;

import com.boutique.common.NotFoundException;

public class PreOrderNotFoundException extends NotFoundException {

	public PreOrderNotFoundException(Long id) {
		super("Pré-commande introuvable : " + id);
	}

}
