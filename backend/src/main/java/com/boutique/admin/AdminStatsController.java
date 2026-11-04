package com.boutique.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

	private final AdminStatsService adminStatsService;

	public AdminStatsController(AdminStatsService adminStatsService) {
		this.adminStatsService = adminStatsService;
	}

	@GetMapping
	public AdminStatsResponse stats() {
		return adminStatsService.stats();
	}

	// ?period=DAY (30 derniers jours, par défaut) ou MONTH (12 derniers mois)
	@GetMapping("/sales")
	public SalesReport sales(@RequestParam(defaultValue = "DAY") SalesPeriod period) {
		return adminStatsService.sales(period);
	}

}
