package com.boutique.preorder;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/preorders")
public class AdminPreOrderController {

	private final PreOrderService preOrderService;

	public AdminPreOrderController(PreOrderService preOrderService) {
		this.preOrderService = preOrderService;
	}

	// Produits en rupture les plus attendus : combien de clients, combien d'articles
	@GetMapping
	public List<PreOrderRepository.WaitingDemand> waitingDemand() {
		return preOrderService.waitingDemand();
	}

}
