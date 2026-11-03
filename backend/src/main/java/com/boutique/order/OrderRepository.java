package com.boutique.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

	// Version naïve : 1 requête pour les commandes, puis 1 requête par commande dès qu'on lit getItems() (N+1)
	List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

	// JOIN FETCH : les commandes et toutes leurs lignes en une seule requête
	@Query("""
			select o from Order o
			left join fetch o.items
			where o.user.id = :userId
			order by o.createdAt desc
			""")
	List<Order> findWithItemsByUserId(Long userId);

}
