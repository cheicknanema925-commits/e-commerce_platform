package com.boutique.user;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	Optional<User> findByExternalId(String externalId);

	long countByRole(Role role);

	long countByRoleAndCreatedAtGreaterThanEqual(Role role, Instant from);

}
