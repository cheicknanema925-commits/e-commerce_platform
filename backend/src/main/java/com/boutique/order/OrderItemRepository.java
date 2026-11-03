package com.boutique.order;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	// Requête dérivée sur un chemin : orderItem.order.user.id, orderItem.product.id et orderItem.order.status
	boolean existsByOrderUserIdAndProductIdAndOrderStatus(Long userId, Long productId, OrderStatus status);

	// Meilleures ventes (commandes annulées exclues) ; la limite vient du Pageable
	@Query("""
			select i.product.id as productId, i.productName as name, sum(i.quantity) as quantity
			from OrderItem i
			where i.order.status <> com.boutique.order.OrderStatus.CANCELLED
			group by i.product.id, i.productName
			order by sum(i.quantity) desc
			""")
	List<ProductSales> bestSellers(Pageable pageable);

	interface ProductSales {

		Long getProductId();

		String getName();

		long getQuantity();

	}

}
