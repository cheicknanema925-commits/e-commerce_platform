package com.boutique.agent.admin;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.boutique.agent.AgentKind;
import com.boutique.agent.StepHandler;
import com.boutique.pricing.PricingService;

@Component
class UndoPriceChangeHandler implements StepHandler {

	static final String TYPE = "UNDO_PRICE_CHANGE";

	private final PricingService pricingService;

	UndoPriceChangeHandler(PricingService pricingService) {
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
		int count = pricingService.undo(UUID.fromString(StepHandler.stringParam(params, "batchId")));
		return count + " prix rétabli(s)";
	}

}
