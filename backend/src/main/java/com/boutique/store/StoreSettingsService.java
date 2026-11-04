package com.boutique.store;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StoreSettingsService {

	private final StoreSettingsRepository repository;

	public StoreSettingsService(StoreSettingsRepository repository) {
		this.repository = repository;
	}

	// La ligne est créée par la migration V15 : son absence serait une erreur d'installation
	public StoreSettings current() {
		return repository.findById(StoreSettings.SINGLE_ROW_ID)
			.orElseThrow(() -> new IllegalStateException("Paramètres de la boutique absents (migration V15)"));
	}

	public StoreSettingsResponse view() {
		return StoreSettingsResponse.from(current());
	}

	@Transactional
	public StoreSettingsResponse update(StoreSettingsRequest request) {
		StoreSettings settings = current();
		settings.update(request.storeName().strip(), request.supportEmail().strip(), request.onlinePaymentEnabled(),
				request.codEnabled(), request.codMaxAmount(), request.codFee(), request.codZones());
		return StoreSettingsResponse.from(settings);
	}

}
