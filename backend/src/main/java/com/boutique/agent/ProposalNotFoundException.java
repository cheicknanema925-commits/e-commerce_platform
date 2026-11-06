package com.boutique.agent;

import com.boutique.common.NotFoundException;

public class ProposalNotFoundException extends NotFoundException {

	public ProposalNotFoundException() {
		super("Action introuvable.");
	}

}
