package com.boutique.store;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoreSettingsController {

	private final StoreSettingsService storeSettingsService;

	public StoreSettingsController(StoreSettingsService storeSettingsService) {
		this.storeSettingsService = storeSettingsService;
	}

	// Public : la page de commande affiche les moyens de paiement disponibles, le plafond et les frais
	@GetMapping("/api/store/settings")
	public StoreSettingsResponse settings() {
		return storeSettingsService.view();
	}

	@PutMapping("/api/admin/settings")
	@PreAuthorize("hasRole('ADMIN')")
	public StoreSettingsResponse update(@Valid @RequestBody StoreSettingsRequest request) {
		return storeSettingsService.update(request);
	}

}
