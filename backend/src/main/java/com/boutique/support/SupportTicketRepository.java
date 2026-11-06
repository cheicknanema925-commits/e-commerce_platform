package com.boutique.support;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

	List<SupportTicket> findTop20ByUserIdOrderByIdDesc(Long userId);

	long countByUserIdAndStatus(Long userId, SupportTicket.Status status);

	// Back-office : le client (relation « vers un ») chargé avec la page, sans N+1
	@EntityGraph(attributePaths = "user")
	Page<SupportTicket> findByStatus(SupportTicket.Status status, Pageable pageable);

	@EntityGraph(attributePaths = "user")
	Page<SupportTicket> findAllBy(Pageable pageable);

	@EntityGraph(attributePaths = "user")
	Optional<SupportTicket> findWithUserById(Long id);

}
