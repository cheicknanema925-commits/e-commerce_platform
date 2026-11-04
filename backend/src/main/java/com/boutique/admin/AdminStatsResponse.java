package com.boutique.admin;

import java.math.BigDecimal;
import java.util.List;

import com.boutique.order.OrderStatus;

// Vue d'ensemble du back-office. Montants « encaissés » = paiements réussis (en ligne confirmés + espèces reçues)
public record AdminStatsResponse(BigDecimal collectedRevenue, long orderCount, long customerCount, Period today,
		Period thisMonth, long awaitingConfirmation, List<StatusCount> ordersByStatus, List<BestSeller> bestSellers,
		List<LowStock> lowStock, Catalog catalog) {

	// Encaissé sur la période et commandes passées (hors annulées)
	public record Period(BigDecimal collected, long orders) {
	}

	public record StatusCount(OrderStatus status, long count) {
	}

	public record BestSeller(Long productId, String name, long quantity) {
	}

	public record LowStock(Long productId, String name, int stock) {
	}

	public record Catalog(long productCount, long outOfStock, BigDecimal stockValue) {
	}

}
