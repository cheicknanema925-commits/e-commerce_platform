package com.boutique.order;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
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

	private final DeliveryRepository deliveryRepository;

	private final ProductRepository productRepository;

	private final UserRepository userRepository;

	private final CartRepository cartRepository;

	private final ApplicationEventPublisher eventPublisher;

	public OrderService(OrderRepository orderRepository, DeliveryRepository deliveryRepository,
			ProductRepository productRepository, UserRepository userRepository, CartRepository cartRepository,
			ApplicationEventPublisher eventPublisher) {
		this.orderRepository = orderRepository;
		this.deliveryRepository = deliveryRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
		this.cartRepository = cartRepository;
		this.eventPublisher = eventPublisher;
	}

	public List<OrderResponse> findOrdersOfUser(Long userId) {
		return orderRepository.findWithItemsByUserId(userId).stream().map(OrderResponse::from).toList();
	}

	public OrderResponse findOrderOfUser(Long userId, Long orderId) {
		return OrderResponse.from(getOwnedOrder(userId, orderId));
	}

	public DeliveryResponse findDeliveryOfUser(Long userId, Long orderId) {
		getOwnedOrder(userId, orderId);
		return deliveryRepository.findByOrderId(orderId)
			.map(DeliveryResponse::from)
			.orElseThrow(() -> new DeliveryNotFoundException(orderId));
	}

	// Pagination en 2 temps : la page de commandes (sans lignes), puis les lignes et clients de CES commandes
	public PageResponse<AdminOrderResponse> findAll(Collection<OrderStatus> statuses, PaymentMethod paymentMethod,
			String search, Pageable pageable) {
		Page<Order> page = orderRepository.findAll(OrderSpecifications.matching(statuses, paymentMethod, search),
				pageable);
		if (page.hasContent()) {
			orderRepository.fetchItems(page.map(Order::getId).getContent());
		}
		return PageResponse.from(page.map(AdminOrderResponse::from));
	}

	public AdminOrderResponse findForAdmin(Long orderId) {
		return AdminOrderResponse.from(getOrder(orderId));
	}

	public DeliveryResponse findDeliveryForAdmin(Long orderId) {
		return deliveryRepository.findByOrderId(orderId)
			.map(DeliveryResponse::from)
			.orElseThrow(() -> new DeliveryNotFoundException(orderId));
	}

	// Commande à partir du panier : prix relus en base, stock réservé, livraison créée, panier vidé. Tout ou rien.
	@Transactional
	public OrderResponse checkout(Long userId, CheckoutRequest request) {
		Cart cart = cartRepository.findByUserId(userId).filter((c) -> !c.isEmpty()).orElseThrow(EmptyCartException::new);
		List<OrderLineRequest> lines = cart.getItems()
			.stream()
			.map((item) -> new OrderLineRequest(item.getProduct().getId(), item.getQuantity()))
			.toList();
		Order order = placeOrder(userId, lines, request.paymentMethod());
		DeliveryAddressRequest address = request.delivery();
		deliveryRepository.save(new Delivery(order, address.addressLine().strip(), address.city().strip(),
				address.phone().strip(), address.latitude(), address.longitude()));
		cart.clear();
		return OrderResponse.from(order);
	}

	@Transactional
	public OrderResponse createOrder(Long userId, List<OrderLineRequest> lines) {
		return createOrder(userId, lines, PaymentMethod.ONLINE);
	}

	@Transactional
	public OrderResponse createOrder(Long userId, List<OrderLineRequest> lines, PaymentMethod paymentMethod) {
		return OrderResponse.from(placeOrder(userId, lines, paymentMethod));
	}

	@Transactional
	public OrderResponse cancelByCustomer(Long userId, Long orderId) {
		Order order = getOwnedOrder(userId, orderId);
		order.changeStatus(OrderStatus.CANCELLED, OrderActor.CUSTOMER);
		restock(order);
		return OrderResponse.from(order);
	}

	// Appel DIRECT pour la livraison : elle doit changer dans la MÊME transaction que la commande (tout ou rien)
	@Transactional
	public AdminOrderResponse changeStatusAsAdmin(Long orderId, OrderStatus target) {
		Order order = getOrder(orderId);
		order.changeStatus(target, OrderActor.ADMIN);
		if (target == OrderStatus.CANCELLED) {
			// Étape 6 : une commande déjà payée devra aussi être remboursée
			restock(order);
		}
		deliveryRepository.findByOrderId(orderId).ifPresent((delivery) -> delivery.follow(target, Instant.now()));
		if (target == OrderStatus.DELIVERED) {
			// Écouté de façon synchrone par le module paiement : les espèces encaissées sont enregistrées
			// dans la même transaction que la remise du colis
			eventPublisher
				.publishEvent(new OrderDeliveredEvent(order.getId(), order.getPaymentMethod(), order.getTotalAmount()));
		}
		return AdminOrderResponse.from(order);
	}

	// ÉVÉNEMENT pour ce qui n'est qu'une conséquence (notifier le client…) : le module commande ne connaît pas
	// ses auditeurs. Appelé uniquement par le traitement du webhook de paiement (étape 6).
	@Transactional
	public void markPaid(Long orderId) {
		Order order = getOrder(orderId);
		order.changeStatus(OrderStatus.PAID, OrderActor.PAYMENT_PROVIDER);
		eventPublisher.publishEvent(new OrderPaidEvent(order.getId(), order.getUser().getEmail(), order.getTotalAmount()));
	}

	// Tout ou rien : si une ligne échoue (stock insuffisant), la commande ET les baisses de stock déjà faites
	// sont annulées par le rollback de la transaction
	private Order placeOrder(Long userId, List<OrderLineRequest> lines, PaymentMethod paymentMethod) {
		User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
		Order order = new Order(user, paymentMethod);
		for (OrderLineRequest line : lines) {
			Product product = productRepository.findById(line.productId())
				.orElseThrow(() -> new ProductNotFoundException(line.productId()));
			product.removeStock(line.quantity());
			order.addItem(product, line.quantity());
		}
		return orderRepository.save(order);
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
