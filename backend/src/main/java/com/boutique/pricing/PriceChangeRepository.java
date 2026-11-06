package com.boutique.pricing;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PriceChangeRepository extends JpaRepository<PriceChange, Long> {

	// Le lot le plus récent encore en place (celui qu'« annule le dernier changement » vise)
	Optional<PriceChange> findFirstByRevertedAtIsNullOrderByIdDesc();

	@EntityGraph(attributePaths = "product")
	List<PriceChange> findByBatchIdAndRevertedAtIsNullOrderById(UUID batchId);

	@EntityGraph(attributePaths = "product")
	@Query("select c from PriceChange c where c.kind = :kind and c.revertedAt is null and c.endsAt <= :now order by c.id")
	List<PriceChange> findDue(PriceChange.Kind kind, Instant now);

	// Journal du back-office : produit et auteur (relations « vers un ») chargés avec la page
	@EntityGraph(attributePaths = { "product", "author" })
	Page<PriceChange> findAllBy(Pageable pageable);

}
