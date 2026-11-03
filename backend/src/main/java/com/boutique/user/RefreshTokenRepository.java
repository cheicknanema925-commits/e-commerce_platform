package com.boutique.user;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	// Révoque d'un coup tous les jetons encore actifs d'un utilisateur (vol suspecté)
	@Modifying
	@Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
	int revokeAllForUser(Long userId, Instant now);

}
