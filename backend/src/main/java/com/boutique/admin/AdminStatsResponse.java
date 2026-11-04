package com.boutique.admin;

import java.math.BigDecimal;
import java.util.List;

import com.boutique.order.OrderStatus;

// Vue d'ensemble du back-office. Montants « encaissés » = paiements réussis (en ligne confirmés + espèces reçues)
public record AdminStatsResponse(BigDecimal collectedRevenue, long orderCount, long customerCount, Period today,
		Period thisMonth, long awaitingConfirmation, BigDecimal averageOrderValue, Payments payments,
		Customers customers, List<StatusCount> ordersByStatus, List<CategorySales> salesByCategory,
		List<BestSeller> bestSellers, List<LowStock> lowStock, Catalog catalog) {

	// Encaissé sur la période et commandes passées (hors annulées)
	public record Period(BigDecimal collected, long orders) {
	}

	// cashToCollect : espèces que les livreurs doivent encore remettre (commandes en espèces ni livrées ni annulées)
	public record Payments(BigDecimal collectedOnline, BigDecimal collectedCash, BigDecimal cashToCollect,
			long cashToCollectOrders) {
	}

	// repeat : clients ayant au moins 2 commandes non annulées
	public record Customers(long newThisMonth, long repeat) {
	}

	public record StatusCount(OrderStatus status, long count) {
	}

	public record CategorySales(String category, long quantity, BigDecimal revenue) {
	}

	// imageUrl : photo principale, null si le produit n'en a pas
	public record BestSeller(Long productId, String name, long quantity, String imageUrl) {
	}

	public record LowStock(Long productId, String name, int stock, String imageUrl) {
	}

	public record Catalog(long productCount, long outOfStock, BigDecimal stockValue) {
	}

}
