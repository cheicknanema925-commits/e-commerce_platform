package com.boutique.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourierRepository extends JpaRepository<Courier, Long> {

	// Actifs d'abord, puis par nom
	List<Courier> findAllByOrderByActiveDescNameAsc();

	// Livraisons en cours par livreur (affectées, pas encore livrées, commande non annulée), en une requête GROUP BY
	@Query("""
			select d.courier.id as courierId, count(d) as count from Delivery d
			where d.courier is not null and d.status <> com.boutique.order.DeliveryStatus.DELIVERED
			  and d.order.status <> com.boutique.order.OrderStatus.CANCELLED
			group by d.courier.id
			""")
	List<OngoingCount> countOngoingDeliveries();

	interface OngoingCount {

		Long getCourierId();

		long getCount();

	}

}
