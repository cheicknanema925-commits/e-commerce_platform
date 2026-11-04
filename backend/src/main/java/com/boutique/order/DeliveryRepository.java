package com.boutique.order;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

	Optional<Delivery> findByOrderId(Long orderId);

	// Livraisons d'une page de commandes, avec leur livreur, en une requête
	@Query("select d from Delivery d left join fetch d.courier where d.order.id in :orderIds")
	List<Delivery> findWithCourierByOrderIdIn(Collection<Long> orderIds);

}
