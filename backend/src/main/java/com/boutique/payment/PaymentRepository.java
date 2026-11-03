package com.boutique.payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByProviderReference(String providerReference);

	List<Payment> findByOrderId(Long orderId);

	// Paiements restés en attente trop longtemps : la réconciliation redemande leur statut au fournisseur
	@Query("select p.id from Payment p where p.status = com.boutique.payment.PaymentStatus.PENDING "
			+ "and p.provider = :provider and p.createdAt < :before")
	List<Long> findPendingIds(String provider, Instant before);

}
