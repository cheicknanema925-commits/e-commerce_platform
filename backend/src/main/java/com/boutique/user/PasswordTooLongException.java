package com.boutique.user;

import com.boutique.common.BadRequestException;

// BCrypt ne lit que les 72 premiers octets (et Spring Security refuse au-delà)
public class PasswordTooLongException extends BadRequestException {

	public PasswordTooLongException() {
		super("Le mot de passe est trop long (72 octets maximum).");
	}

}
