package com.boutique.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByProviderReference(String providerReference);

	List<Payment> findByOrderId(Long orderId);

	// Argent réellement encaissé : paiements en ligne confirmés + espèces reçues à la livraison
	@Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = com.boutique.payment.PaymentStatus.SUCCEEDED")
	BigDecimal totalCollected();

	@Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = com.boutique.payment.PaymentStatus.SUCCEEDED "
			+ "and p.updatedAt >= :from")
	BigDecimal collectedSince(Instant from);

	// Encaissements par jour ou par mois, à l'heure de la boutique. updated_at = moment où le paiement a réussi
	// (confirmation du fournisseur ou espèces reçues à la livraison)
	@Query(value = """
			select to_char(date_trunc(:unit, p.updated_at at time zone :zone), 'YYYY-MM-DD') as period,
			       sum(p.amount) as amount
			from payments p
			where p.status = 'SUCCEEDED' and p.updated_at >= :from
			group by 1
			""", nativeQuery = true)
	List<PeriodAmount> collectedByPeriod(String unit, String zone, Instant from);

	// Paiements restés en attente trop longtemps : la réconciliation redemande leur statut au fournisseur
	@Query("select p.id from Payment p where p.status = com.boutique.payment.PaymentStatus.PENDING "
			+ "and p.provider = :provider and p.createdAt < :before")
	List<Long> findPendingIds(String provider, Instant before);

	interface PeriodAmount {

		String getPeriod();

		BigDecimal getAmount();

	}

}
