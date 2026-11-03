package com.boutique.admin;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.admin.AdminStatsResponse.BestSeller;
import com.boutique.admin.AdminStatsResponse.LowStock;
import com.boutique.admin.AdminStatsResponse.StatusCount;
import com.boutique.catalog.ProductRepository;
import com.boutique.order.OrderItemRepository;
import com.boutique.order.OrderRepository;
import com.boutique.payment.PaymentRepository;
import com.boutique.user.Role;
import com.boutique.user.UserRepository;

// Tableau de bord : chaque chiffre est calculé PAR LA BASE (sum, count, group by), jamais en chargeant
// toutes les commandes en mémoire. Ce module dépend des autres ; aucun ne dépend de lui (pas de cycle).
@Service
@Transactional(readOnly = true)
public class AdminStatsService {

	static final int LOW_STOCK_THRESHOLD = 5;

	private final PaymentRepository paymentRepository;

	private final OrderRepository orderRepository;

	private final OrderItemRepository orderItemRepository;

	private final ProductRepository productRepository;

	private final UserRepository userRepository;

	public AdminStatsService(PaymentRepository paymentRepository, OrderRepository orderRepository,
			OrderItemRepository orderItemRepository, ProductRepository productRepository,
			UserRepository userRepository) {
		this.paymentRepository = paymentRepository;
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
	}

	public AdminStatsResponse stats() {
		return new AdminStatsResponse(paymentRepository.totalCollected(), orderRepository.count(),
				userRepository.countByRole(Role.CUSTOMER),
				orderRepository.countByStatus().stream().map((s) -> new StatusCount(s.getStatus(), s.getCount())).toList(),
				orderItemRepository.bestSellers(PageRequest.of(0, 5))
					.stream()
					.map((p) -> new BestSeller(p.getProductId(), p.getName(), p.getQuantity()))
					.toList(),
				productRepository.findTop10ByStockLessThanEqualOrderByStockAsc(LOW_STOCK_THRESHOLD)
					.stream()
					.map((p) -> new LowStock(p.getId(), p.getName(), p.getStock()))
					.toList());
	}

}
