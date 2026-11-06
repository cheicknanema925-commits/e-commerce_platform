package com.boutique.pricing;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.common.PageResponse;

// Journal des changements de prix, consultable dans le back-office (/api/admin/** : rôle ADMIN)
@RestController
@RequestMapping("/api/admin/price-changes")
public class AdminPriceChangeController {

	private final PricingService pricingService;

	public AdminPriceChangeController(PricingService pricingService) {
		this.pricingService = pricingService;
	}

	@GetMapping
	public PageResponse<PriceChangeResponse> history(
			@PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return pricingService.history(pageable);
	}

}
