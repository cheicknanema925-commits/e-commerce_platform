package com.boutique.user;

import com.boutique.common.UnauthorizedException;

// Même message pour un email inconnu, un mauvais mot de passe ou un compte verrouillé :
// un attaquant ne peut pas savoir lequel des trois cas s'est produit
public class InvalidCredentialsException extends UnauthorizedException {

	public InvalidCredentialsException() {
		super("Email ou mot de passe incorrect.");
	}

}
