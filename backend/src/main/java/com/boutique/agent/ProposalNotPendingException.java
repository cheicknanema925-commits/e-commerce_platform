package com.boutique.agent;

import com.boutique.common.ConflictException;

public class ProposalNotPendingException extends ConflictException {

	public ProposalNotPendingException() {
		super("Cette action a déjà été confirmée, annulée ou a expiré. Demandez à l'assistant de la préparer à nouveau.");
	}

}
