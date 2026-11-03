package com.boutique.order;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.catalog.Product;
import com.boutique.catalog.ProductNotFoundException;
import com.boutique.catalog.ProductRepository;
import com.boutique.user.User;
import com.boutique.user.UserNotFoundException;
import com.boutique.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {

	private final OrderRepository orderRepository;

	private final ProductRepository productRepository;

	private final UserRepository userRepository;

	public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
			UserRepository userRepository) {
		this.orderRepository = orderRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
	}

	public List<OrderResponse> findOrdersOfUser(Long userId) {
		return orderRepository.findWithItemsByUserId(userId).stream().map(OrderResponse::from).toList();
	}

	// Tout ou rien : si une ligne échoue (stock insuffisant), la commande ET les baisses de stock déjà faites
	// sont annulées par le rollback de la transaction
	@Transactional
	public OrderResponse createOrder(Long userId, List<OrderLineRequest> lines) {
		User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
		Order order = new Order(user);
		for (OrderLineRequest line : lines) {
			Product product = productRepository.findById(line.productId())
				.orElseThrow(() -> new ProductNotFoundException(line.productId()));
			product.removeStock(line.quantity());
			order.addItem(product, line.quantity());
		}
		return OrderResponse.from(orderRepository.save(order));
	}

}
