package com.boutique.order;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.cart.Cart;
import com.boutique.cart.CartRepository;
import com.boutique.cart.EmptyCartException;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductNotFoundException;
import com.boutique.catalog.ProductRepository;
import com.boutique.common.PageResponse;
import com.boutique.user.User;
import com.boutique.user.UserNotFoundException;
import com.boutique.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {

	private final OrderRepository orderRepository;

	private final ProductRepository productRepository;

	private final UserRepository userRepository;

	private final CartRepository cartRepository;

	public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
			UserRepository userRepository, CartRepository cartRepository) {
		this.orderRepository = orderRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
		this.cartRepository = cartRepository;
	}

	public List<OrderResponse> findOrdersOfUser(Long userId) {
		return orderRepository.findWithItemsByUserId(userId).stream().map(OrderResponse::from).toList();
	}

	public OrderResponse findOrderOfUser(Long userId, Long orderId) {
		return OrderResponse.from(getOwnedOrder(userId, orderId));
	}

	// Pagination en 2 temps : la page de commandes (sans lignes), puis les lignes de CES commandes en 1 requête
	public PageResponse<OrderResponse> findAll(OrderStatus status, Pageable pageable) {
		Page<Order> page = (status == null) ? orderRepository.findAll(pageable)
				: orderRepository.findByStatus(status, pageable);
		if (page.hasContent()) {
			orderRepository.fetchItems(page.map(Order::getId).getContent());
		}
		return PageResponse.from(page.map(OrderResponse::from));
	}

	// Commande à partir du panier : prix relus en base, stock réservé, panier vidé. Tout ou rien.
	@Transactional
	public OrderResponse checkout(Long userId) {
		Cart cart = cartRepository.findByUserId(userId).filter((c) -> !c.isEmpty()).orElseThrow(EmptyCartException::new);
		List<OrderLineRequest> lines = cart.getItems()
			.stream()
			.map((item) -> new OrderLineRequest(item.getProduct().getId(), item.getQuantity()))
			.toList();
		OrderResponse order = createOrder(userId, lines);
		cart.clear();
		return order;
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

	@Transactional
	public OrderResponse cancelByCustomer(Long userId, Long orderId) {
		Order order = getOwnedOrder(userId, orderId);
		order.changeStatus(OrderStatus.CANCELLED, OrderActor.CUSTOMER);
		restock(order);
		return OrderResponse.from(order);
	}

	@Transactional
	public OrderResponse changeStatusAsAdmin(Long orderId, OrderStatus target) {
		Order order = getOrder(orderId);
		order.changeStatus(target, OrderActor.ADMIN);
		if (target == OrderStatus.CANCELLED) {
			// Étape 6 : une commande déjà payée devra aussi être remboursée
			restock(order);
		}
		return OrderResponse.from(order);
	}

	// Appelé uniquement par le traitement du webhook de paiement (étape 6), jamais par une route publique
	@Transactional
	public void markPaid(Long orderId) {
		getOrder(orderId).changeStatus(OrderStatus.PAID, OrderActor.PAYMENT_PROVIDER);
	}

	private void restock(Order order) {
		order.getItems().forEach((item) -> item.getProduct().addStock(item.getQuantity()));
	}

	// 404 (et pas 403) pour la commande d'un autre : on ne révèle même pas qu'elle existe
	private Order getOwnedOrder(Long userId, Long orderId) {
		return orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(() -> new OrderNotFoundException(orderId));
	}

	private Order getOrder(Long orderId) {
		return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
	}

}
