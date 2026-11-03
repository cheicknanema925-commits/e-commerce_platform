package com.boutique.security;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRolesConverterTest {

	private final KeycloakRealmRolesConverter converter = new KeycloakRealmRolesConverter();

	@Test
	void realmRolesBecomeSpringRoles() {
		Jwt jwt = Jwt.withTokenValue("jeton")
			.header("alg", "RS256")
			.claim("realm_access", Map.of("roles", List.of("ADMIN", "offline_access")))
			.build();

		assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
			.containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_offline_access");
	}

	@Test
	void tokenWithoutRealmAccessHasNoRole() {
		Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "RS256").claim("sub", "123").build();

		assertThat(converter.convert(jwt)).isEmpty();
	}

}
