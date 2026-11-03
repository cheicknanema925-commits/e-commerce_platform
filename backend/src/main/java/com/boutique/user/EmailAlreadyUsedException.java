package com.boutique.user;

import com.boutique.common.ConflictException;

public class EmailAlreadyUsedException extends ConflictException {

	public EmailAlreadyUsedException() {
		super("Un compte existe déjà avec cet email.");
	}

}
