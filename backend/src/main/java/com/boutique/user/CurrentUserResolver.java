package com.boutique.user;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.stereotype.Component;

import com.boutique.security.SecurityProperties;

// L'identité de l'utilisateur vient TOUJOURS du jeton signé, jamais d'un paramètre envoyé par le client (protection IDOR).
// Nos jetons : « sub » est déjà l'id local. Jetons Keycloak : « sub » est un UUID Keycloak, relié au compte local.
@Component
public class CurrentUserResolver {

	private final SecurityProperties properties;

	private final ExternalAccountService externalAccountService;

	public CurrentUserResolver(SecurityProperties properties, ExternalAccountService externalAccountService) {
		this.properties = properties;
		this.externalAccountService = externalAccountService;
	}

	public Long userId(Jwt jwt) {
		if (properties.jwt().issuer().equals(jwt.getClaimAsString(JwtClaimNames.ISS))) {
			return Long.valueOf(jwt.getSubject());
		}
		return externalAccountService.findOrCreate(jwt);
	}

}
