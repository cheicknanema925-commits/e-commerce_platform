package com.boutique.preorder;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PreOrderRepository extends JpaRepository<PreOrder, Long> {

	Optional<PreOrder> findByUserIdAndProductIdAndStatus(Long userId, Long productId, PreOrder.Status status);

	Optional<PreOrder> findByIdAndUserId(Long id, Long userId);

	@Query("select p from PreOrder p join fetch p.product where p.user.id = :userId order by p.id desc")
	List<PreOrder> findWithProductByUserId(Long userId);

	@Query("select p from PreOrder p join fetch p.user where p.product.id = :productId and p.status = :status")
	List<PreOrder> findWithUserByProductIdAndStatus(Long productId, PreOrder.Status status);

	// Back-office : la demande en attente par produit, calculée en base (aide à décider d'un réassort)
	@Query("""
			select p.product.id as productId, p.product.name as productName, count(p) as requests, sum(p.quantity) as quantity
			from PreOrder p where p.status = :status
			group by p.product.id, p.product.name order by count(p) desc, p.product.name
			""")
	List<WaitingDemand> demandByStatus(PreOrder.Status status);

	interface WaitingDemand {

		Long getProductId();

		String getProductName();

		long getRequests();

		long getQuantity();

	}

}
