package com.boutique.admin;

import java.math.BigDecimal;
import java.util.List;

import com.boutique.order.OrderStatus;

public record AdminStatsResponse(BigDecimal collectedRevenue, long orderCount, long customerCount,
		List<StatusCount> ordersByStatus, List<BestSeller> bestSellers, List<LowStock> lowStock) {

	public record StatusCount(OrderStatus status, long count) {
	}

	public record BestSeller(Long productId, String name, long quantity) {
	}

	public record LowStock(Long productId, String name, int stock) {
	}

}
