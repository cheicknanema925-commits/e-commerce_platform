package com.boutique.user;

import com.boutique.common.NotFoundException;

public class UserNotFoundException extends NotFoundException {

	public UserNotFoundException(Long id) {
		super("Utilisateur introuvable : " + id);
	}

}
