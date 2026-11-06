package com.boutique.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Ce que le changement fera, produit par produit (aperçu ET contrôle au moment de l'appliquer)
public record PricePlan(PriceChange.Kind kind, List<Line> lines, Instant endsAt) {

	public record Line(Long productId, String productName, BigDecimal oldPrice, BigDecimal newPrice,
			BigDecimal newCompareAtPrice, Integer discountPercent) {
	}

	// Prix lus au moment de l'aperçu : au clic, tout est refusé si l'un d'eux a changé
	public Map<Long, BigDecimal> expectedOldPrices() {
		return lines.stream().collect(Collectors.toMap(Line::productId, Line::oldPrice));
	}

}
