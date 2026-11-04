package com.boutique.admin;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.boutique.catalog.ProductService;
import com.boutique.common.CsvWriter;
import com.boutique.common.PageResponse;
import com.boutique.newsletter.NewsletterService;
import com.boutique.order.OrderService;
import com.boutique.order.OrderStatus;
import com.boutique.order.PaymentMethod;

// Exports CSV du back-office, construits page par page (500 lignes à la fois) pour ne jamais charger toute la base
@Service
public class AdminExportService {

	private static final int BATCH = 500;

	private final ProductService productService;

	private final OrderService orderService;

	private final AdminCustomerService adminCustomerService;

	private final NewsletterService newsletterService;

	public AdminExportService(ProductService productService, OrderService orderService,
			AdminCustomerService adminCustomerService, NewsletterService newsletterService) {
		this.productService = productService;
		this.orderService = orderService;
		this.adminCustomerService = adminCustomerService;
		this.newsletterService = newsletterService;
	}

	public byte[] products() {
		CsvWriter csv = new CsvWriter().row(List.of("Référence", "Nom", "Marque", "Catégorie", "Prix (FCFA)",
				"Prix barré (FCFA)", "Remise (%)", "Stock", "Photos"));
		forEachPage((page) -> productService.findAll(null, null, false, PageRequest.of(page, BATCH, Sort.by("id"))),
				(p) -> csv.row(List.of(p.sku(), p.name(), nullable(p.brand()), p.categoryName(), p.price(),
						nullable(p.compareAtPrice()), nullable(p.discountPercent()), p.stock(), p.images().size())));
		return csv.toBytes();
	}

	public byte[] orders() {
		CsvWriter csv = new CsvWriter().row(List.of("N°", "Date", "Client", "Email", "Statut", "Paiement", "Articles",
				"Frais (FCFA)", "Total (FCFA)", "Livreur"));
		forEachPage(
				(page) -> orderService.findAll(null, null, null, PageRequest.of(page, BATCH, Sort.by(Sort.Direction.DESC, "id"))),
				(o) -> csv.row(List.of(o.id(), o.createdAt(), o.customerName(), o.customerEmail(),
						statusLabel(o.status(), o.paymentMethod()), paymentLabel(o.paymentMethod()), o.items().stream().mapToInt((item) -> item.quantity()).sum(), o.serviceFee(),
						o.totalAmount(), nullable(o.courierName()))));
		return csv.toBytes();
	}

	public byte[] customers() {
		CsvWriter csv = new CsvWriter().row(List.of("Nom", "Email", "Inscrit le", "Commandes", "Total commandé (FCFA)",
				"Dernière commande"));
		forEachPage((page) -> adminCustomerService.findAll(null, page, BATCH),
				(c) -> csv.row(List.of(c.fullName(), c.email(), c.createdAt(), c.orderCount(), c.totalSpent(),
						nullable(c.lastOrderAt()))));
		return csv.toBytes();
	}

	public byte[] newsletter() {
		CsvWriter csv = new CsvWriter().row(List.of("Email", "Inscrit le"));
		forEachPage((page) -> PageResponse.from(newsletterService.subscribers(PageRequest.of(page, BATCH, Sort.by("id")))),
				(s) -> csv.row(List.of(s.getEmail(), s.getCreatedAt())));
		return csv.toBytes();
	}

	private static <T> void forEachPage(IntFunction<PageResponse<T>> loader, Consumer<T> writer) {
		for (int page = 0;; page++) {
			PageResponse<T> result = loader.apply(page);
			result.content().forEach(writer);
			if (page + 1 >= result.totalPages()) {
				return;
			}
		}
	}

	// Mêmes libellés que l'interface : le fichier est lu par des humains, pas par un programme
	static String statusLabel(OrderStatus status, PaymentMethod method) {
		return switch (status) {
			case CREATED -> (method == PaymentMethod.ONLINE) ? "En attente de paiement" : "En attente de confirmation";
			case PAID -> "Payée";
			case PREPARING -> "En préparation";
			case SHIPPED -> "Expédiée";
			case DELIVERED -> "Livrée";
			case CANCELLED -> "Annulée";
		};
	}

	static String paymentLabel(PaymentMethod method) {
		return (method == PaymentMethod.ONLINE) ? "En ligne" : "Espèces à la livraison";
	}

	// List.of refuse null : une cellule vide s'écrit avec une chaîne vide
	private static Object nullable(Object value) {
		return (value == null) ? "" : value;
	}

}
