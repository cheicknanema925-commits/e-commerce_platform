package com.boutique.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.admin.AdminStatsResponse.BestSeller;
import com.boutique.admin.AdminStatsResponse.Catalog;
import com.boutique.admin.AdminStatsResponse.CategorySales;
import com.boutique.admin.AdminStatsResponse.Customers;
import com.boutique.admin.AdminStatsResponse.LowStock;
import com.boutique.admin.AdminStatsResponse.Payments;
import com.boutique.admin.AdminStatsResponse.Period;
import com.boutique.admin.AdminStatsResponse.StatusCount;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductRepository;
import com.boutique.order.OrderItemRepository;
import com.boutique.order.OrderRepository;
import com.boutique.order.OrderStatus;
import com.boutique.order.PaymentMethod;
import com.boutique.payment.PaymentRepository;
import com.boutique.payment.PaymentService;
import com.boutique.user.Role;
import com.boutique.user.UserRepository;

// Tableau de bord : chaque chiffre est calculé PAR LA BASE (sum, count, group by), jamais en chargeant
// toutes les commandes en mémoire. Ce module dépend des autres ; aucun ne dépend de lui (pas de cycle).
@Service
@Transactional(readOnly = true)
public class AdminStatsService {

	static final int LOW_STOCK_THRESHOLD = 5;

	// « Aujourd'hui » et « ce mois-ci » se comptent à l'heure de la boutique, pas à celle du serveur
	static final ZoneId SHOP_ZONE = ZoneId.of("Africa/Dakar");

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
		LocalDate today = LocalDate.now(SHOP_ZONE);
		Instant monthStart = today.withDayOfMonth(1).atStartOfDay(SHOP_ZONE).toInstant();
		BigDecimal collected = paymentRepository.totalCollected();
		BigDecimal collectedCash = paymentRepository.collectedBy(PaymentService.CASH);
		OrderRepository.AmountCount cashToCollect = orderRepository.cashToCollect();
		Double average = orderRepository.averageOrderValue();

		List<OrderItemRepository.ProductSales> sales = orderItemRepository.bestSellers(PageRequest.of(0, 5));
		Map<Long, String> images = mainImages(sales.stream().map(OrderItemRepository.ProductSales::getProductId).toList());
		List<Product> lowStock = productRepository.findTop10ByStockLessThanEqualOrderByStockAsc(LOW_STOCK_THRESHOLD);

		return new AdminStatsResponse(collected, orderRepository.count(), userRepository.countByRole(Role.CUSTOMER),
				periodSince(today), periodSince(today.withDayOfMonth(1)),
				orderRepository.countByStatusAndPaymentMethod(OrderStatus.CREATED, PaymentMethod.CASH_ON_DELIVERY),
				(average == null) ? BigDecimal.ZERO : BigDecimal.valueOf(average).setScale(0, RoundingMode.HALF_UP),
				new Payments(collected.subtract(collectedCash), collectedCash, cashToCollect.getAmount(),
						cashToCollect.getCount()),
				new Customers(userRepository.countByRoleAndCreatedAtGreaterThanEqual(Role.CUSTOMER, monthStart),
						orderRepository.countRepeatCustomers()),
				orderRepository.countByStatus().stream().map((s) -> new StatusCount(s.getStatus(), s.getCount())).toList(),
				orderItemRepository.salesByCategory()
					.stream()
					.map((c) -> new CategorySales(c.getCategory(), c.getQuantity(), c.getRevenue()))
					.toList(),
				sales.stream()
					.map((p) -> new BestSeller(p.getProductId(), p.getName(), p.getQuantity(), images.get(p.getProductId())))
					.toList(),
				lowStock.stream().map((p) -> new LowStock(p.getId(), p.getName(), p.getStock(), p.getMainImageUrl())).toList(),
				new Catalog(productRepository.count(), productRepository.countByStock(0),
						productRepository.stockValue()));
	}

	// Photo principale de quelques produits : 1 requête pour les produits, 1 pour leurs photos (@BatchSize)
	private Map<Long, String> mainImages(List<Long> productIds) {
		Map<Long, String> urls = new HashMap<>();
		productRepository.findAllById(productIds).forEach((p) -> urls.put(p.getId(), p.getMainImageUrl()));
		return urls;
	}

	// 30 derniers jours (DAY) ou 12 derniers mois (MONTH), aujourd'hui compris. Deux requêtes GROUP BY, puis on
	// complète en Java les périodes sans vente : la base ne renvoie que les jours où il s'est passé quelque chose.
	public SalesReport sales(SalesPeriod period) {
		LocalDate today = LocalDate.now(SHOP_ZONE);
		LocalDate first = (period == SalesPeriod.DAY) ? today.minusDays(29) : today.withDayOfMonth(1).minusMonths(11);
		Instant from = first.atStartOfDay(SHOP_ZONE).toInstant();
		String zone = SHOP_ZONE.getId();

		Map<LocalDate, BigDecimal> collected = paymentRepository.collectedByPeriod(period.sqlUnit, zone, from)
			.stream()
			.collect(Collectors.toMap((p) -> LocalDate.parse(p.getPeriod()), (p) -> p.getAmount()));
		Map<LocalDate, Long> orders = orderRepository.countByPeriod(period.sqlUnit, zone, from)
			.stream()
			.collect(Collectors.toMap((p) -> LocalDate.parse(p.getPeriod()), (p) -> p.getCount()));

		UnaryOperator<LocalDate> next = (period == SalesPeriod.DAY) ? (d) -> d.plusDays(1) : (d) -> d.plusMonths(1);
		List<SalesReport.Point> points = Stream.iterate(first, (d) -> !d.isAfter(today), next)
			.map((d) -> new SalesReport.Point(d, collected.getOrDefault(d, BigDecimal.ZERO), orders.getOrDefault(d, 0L)))
			.toList();
		return new SalesReport(period, points);
	}

	private Period periodSince(LocalDate day) {
		Instant from = day.atStartOfDay(SHOP_ZONE).toInstant();
		return new Period(paymentRepository.collectedSince(from),
				orderRepository.countByCreatedAtGreaterThanEqualAndStatusNot(from, OrderStatus.CANCELLED));
	}

}
