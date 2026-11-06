package com.boutique.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Soit une remise en pourcentage (promotion, date de fin obligatoire), soit un nouveau prix pour UN produit
public record PriceChangeRequest(List<Long> productIds, Integer percent, BigDecimal newPrice, LocalDate endDate) {
}
