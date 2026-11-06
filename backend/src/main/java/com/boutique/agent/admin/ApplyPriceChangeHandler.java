package com.boutique.agent.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.boutique.agent.AgentKind;
import com.boutique.agent.StepHandler;
import com.boutique.pricing.PriceChangeRequest;
import com.boutique.pricing.PricingService;

// Exécuté au clic de l'admin. PricingService recalcule tout et revérifie les limites ET les prix attendus
@Component
class ApplyPriceChangeHandler implements StepHandler {

	static final String TYPE = "APPLY_PRICE_CHANGE";

	private final PricingService pricingService;

	ApplyPriceChangeHandler(PricingService pricingService) {
		this.pricingService = pricingService;
	}

	@Override
	public String type() {
		return TYPE;
	}

	@Override
	public AgentKind agent() {
		return AgentKind.ADMIN;
	}

	@Override
	public String execute(Long userId, Map<String, Object> params) {
		if (!(params.get("productIds") instanceof List<?> ids) || !(params.get("expectedOldPrices") instanceof Map<?, ?> expected)) {
			throw new IllegalArgumentException("Paramètres du changement de prix incomplets");
		}
		Map<Long, BigDecimal> expectedOldPrices = new HashMap<>();
		expected.forEach((id, price) -> expectedOldPrices.put(Long.valueOf(String.valueOf(id)), new BigDecimal(String.valueOf(price))));
		PriceChangeRequest request = new PriceChangeRequest(ids.stream().map((id) -> ((Number) id).longValue()).toList(),
				params.get("percent") instanceof Number percent ? percent.intValue() : null,
				params.get("newPrice") instanceof String price ? new BigDecimal(price) : null,
				params.get("endDate") instanceof String date ? LocalDate.parse(date) : null);
		int count = pricingService.apply(userId, request, expectedOldPrices).lines().size();
		return count + " prix modifié(s)";
	}

}
