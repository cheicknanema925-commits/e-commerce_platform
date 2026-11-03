package com.boutique.order;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	// Protection IDOR : la commande n'est trouvée que si elle appartient à cet utilisateur
	Optional<Order> findByIdAndUserId(Long id, Long userId);

	Page<Order> findByStatus(OrderStatus status, Pageable pageable);

	// 2e temps de la pagination : charger les lignes ET le client des commandes de la page (jamais de JOIN FETCH paginé)
	@Query("select distinct o from Order o left join fetch o.items join fetch o.user where o.id in :ids")
	List<Order> fetchItems(Collection<Long> ids);

	@Query("select o.status as status, count(o) as count from Order o group by o.status")
	List<StatusCount> countByStatus();

	// Projection : Spring Data remplit cette interface avec les colonnes nommées (alias) de la requête
	interface StatusCount {

		OrderStatus getStatus();

		long getCount();

	}

}
