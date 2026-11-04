package com.boutique.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

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

	// 2e temps de la pagination : charger les lignes ET le client des commandes de la page (jamais de JOIN FETCH paginé)
	@Query("select distinct o from Order o left join fetch o.items join fetch o.user where o.id in :ids")
	List<Order> fetchItems(Collection<Long> ids);

	@Query("select o.status as status, count(o) as count from Order o group by o.status")
	List<StatusCount> countByStatus();

	// Commandes en espèces à confirmer, commandes en ligne en attente de paiement…
	long countByStatusAndPaymentMethod(OrderStatus status, PaymentMethod paymentMethod);

	// Montant moyen d'une commande (annulées exclues) ; avg renvoie un Double, null s'il n'y a aucune commande
	@Query("select avg(o.totalAmount) from Order o where o.status <> com.boutique.order.OrderStatus.CANCELLED")
	Double averageOrderValue();

	// Espèces encore à récupérer : commandes payables à la livraison, ni livrées ni annulées
	@Query("""
			select count(o) as count, coalesce(sum(o.totalAmount), 0) as amount from Order o
			where o.paymentMethod = com.boutique.order.PaymentMethod.CASH_ON_DELIVERY
			  and o.status in (com.boutique.order.OrderStatus.CREATED, com.boutique.order.OrderStatus.PREPARING,
			                   com.boutique.order.OrderStatus.SHIPPED)
			""")
	AmountCount cashToCollect();

	// Clients fidèles : au moins 2 commandes non annulées (sous-requête corrélée)
	@Query("""
			select count(u) from User u
			where (select count(o) from Order o where o.user = u
			       and o.status <> com.boutique.order.OrderStatus.CANCELLED) >= 2
			""")
	long countRepeatCustomers();

	interface AmountCount {

		long getCount();

		BigDecimal getAmount();

	}

	// Commandes passées depuis une date (aujourd'hui, ce mois-ci), annulées exclues
	long countByCreatedAtGreaterThanEqualAndStatusNot(Instant from, OrderStatus excluded);

	// Requête SQL native : date_trunc (PostgreSQL) ramène chaque date au début de son jour ou de son mois,
	// à l'heure de la boutique (:zone) ; le GROUP BY compte les commandes de chaque période
	@Query(value = """
			select to_char(date_trunc(:unit, o.created_at at time zone :zone), 'YYYY-MM-DD') as period,
			       count(*) as count
			from orders o
			where o.status <> 'CANCELLED' and o.created_at >= :from
			group by 1
			""", nativeQuery = true)
	List<PeriodCount> countByPeriod(String unit, String zone, Instant from);

	// Projection : Spring Data remplit cette interface avec les colonnes nommées (alias) de la requête
	interface StatusCount {

		OrderStatus getStatus();

		long getCount();

	}

	interface PeriodCount {

		String getPeriod();

		long getCount();

	}

}
