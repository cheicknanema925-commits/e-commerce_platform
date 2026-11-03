package com.boutique.user;

import com.boutique.common.UnauthorizedException;

public class InvalidRefreshTokenException extends UnauthorizedException {

	public InvalidRefreshTokenException() {
		super("Session expirée ou invalide : reconnectez-vous.");
	}

}
