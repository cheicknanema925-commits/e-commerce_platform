package com.boutique.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceChangeResponse(Long id, UUID batchId, Long productId, String productName, String authorName,
		PriceChange.Kind kind, BigDecimal oldPrice, BigDecimal newPrice, BigDecimal oldCompareAtPrice,
		BigDecimal newCompareAtPrice, Instant endsAt, Instant createdAt, Instant revertedAt,
		PriceChange.RevertReason revertReason) {

	static PriceChangeResponse from(PriceChange change) {
		return new PriceChangeResponse(change.getId(), change.getBatchId(), change.getProduct().getId(),
				change.getProduct().getName(), change.getAuthor() == null ? null : change.getAuthor().getFullName(),
				change.getKind(), change.getOldPrice(), change.getNewPrice(), change.getOldCompareAtPrice(),
				change.getNewCompareAtPrice(), change.getEndsAt(), change.getCreatedAt(), change.getRevertedAt(),
				change.getRevertReason());
	}

}
