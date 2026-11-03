package com.boutique.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	// Requête dérivée sur un chemin : orderItem.order.user.id, orderItem.product.id et orderItem.order.status
	boolean existsByOrderUserIdAndProductIdAndOrderStatus(Long userId, Long productId, OrderStatus status);

}
