package com.boutique.agent;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface AgentProposalRepository extends JpaRepository<AgentProposal, UUID> {

	// Une proposition n'est trouvée que pour SON auteur et SON agent (IDOR, et jamais une action admin côté client)
	Optional<AgentProposal> findByIdAndUserIdAndAgent(UUID id, Long userId, AgentKind agent);

	// Réservation atomique : UN seul appel passe de PENDING à l'état demandé (un double-clic n'exécute pas deux fois)
	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("""
			update AgentProposal p set p.status = :target, p.decidedAt = :now
			where p.id = :id and p.status = com.boutique.agent.AgentProposal.Status.PENDING and p.expiresAt > :now
			""")
	int claim(UUID id, AgentProposal.Status target, Instant now);

}
