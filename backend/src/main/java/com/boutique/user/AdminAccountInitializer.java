package com.boutique.user;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.boutique.security.SecurityProperties;

// Crée le premier compte ADMIN au démarrage à partir de ADMIN_EMAIL / ADMIN_PASSWORD (variables d'environnement).
// Aucun compte par défaut écrit dans le code ; rien n'est créé si les variables sont vides.
@Component
class AdminAccountInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final SecurityProperties properties;

	AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
			SecurityProperties properties) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		SecurityProperties.Admin admin = properties.admin();
		if (!StringUtils.hasText(admin.email()) || !StringUtils.hasText(admin.password())) {
			return;
		}
		String email = admin.email().strip().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmail(email)) {
			return;
		}
		userRepository.save(new User(email, "Administrateur", Role.ADMIN, passwordEncoder.encode(admin.password())));
		log.info("Compte administrateur créé : {}", email);
	}

}
