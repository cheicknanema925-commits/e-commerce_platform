package com.boutique.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

// Keycloak range les rôles du realm dans un claim imbriqué : { "realm_access": { "roles": ["ADMIN", ...] } }.
// On les transforme en autorisations Spring ROLE_ADMIN, ROLE_CUSTOMER… : hasRole("ADMIN") marche comme avec nos jetons.
public class KeycloakRealmRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
		if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
			return List.of();
		}
		return roles.stream()
			.map(String::valueOf)
			.map((role) -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
			.toList();
	}

}
