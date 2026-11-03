package com.boutique.user;

import com.boutique.common.ConflictException;

// Un compte Keycloak ne peut pas être relié à un compte local (email absent, non vérifié ou déjà relié ailleurs)
public class AccountLinkException extends ConflictException {

	public AccountLinkException(String message) {
		super(message);
	}

}
