package com.boutique.user;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// Comptes Keycloak : à la première requête, on crée (ou relie) le compte local qui portera commandes, panier et avis.
// On appelle ça le provisionnement « juste à temps » (just-in-time provisioning).
@Service
@Transactional
public class ExternalAccountService {

	private final UserRepository userRepository;

	public ExternalAccountService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public Long findOrCreate(Jwt jwt) {
		return userRepository.findByExternalId(jwt.getSubject()).map(User::getId).orElseGet(() -> provision(jwt));
	}

	private Long provision(Jwt jwt) {
		String email = jwt.getClaimAsString("email");
		if (!StringUtils.hasText(email)) {
			throw new AccountLinkException("Le jeton Keycloak ne contient pas d'email : impossible de créer le compte.");
		}
		email = email.strip().toLowerCase(Locale.ROOT);

		User existing = userRepository.findByEmail(email).orElse(null);
		if (existing != null) {
			// Relier un compte local existant SEULEMENT si Keycloak a vérifié que la personne possède cet email :
			// sinon n'importe qui pourrait créer un compte Keycloak avec l'email d'un autre et récupérer ses commandes
			if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")) || existing.getExternalId() != null) {
				throw new AccountLinkException("Un compte existe déjà avec cet email et ne peut pas être relié automatiquement.");
			}
			existing.linkExternalAccount(jwt.getSubject());
			return existing.getId();
		}

		User user = new User(email, fullName(jwt), hasRealmRole(jwt, "ADMIN") ? Role.ADMIN : Role.CUSTOMER);
		user.linkExternalAccount(jwt.getSubject());
		return userRepository.save(user).getId();
	}

	private static String fullName(Jwt jwt) {
		String name = jwt.getClaimAsString("name");
		return StringUtils.hasText(name) ? name : jwt.getClaimAsString("preferred_username");
	}

	private static boolean hasRealmRole(Jwt jwt, String role) {
		Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
		return realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles && roles.contains(role);
	}

}
