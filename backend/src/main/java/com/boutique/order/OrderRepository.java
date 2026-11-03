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

	// 2e temps de la pagination : charger les lignes des commandes de la page (on ne pagine jamais un JOIN FETCH)
	@Query("select distinct o from Order o left join fetch o.items where o.id in :ids")
	List<Order> fetchItems(Collection<Long> ids);

}
