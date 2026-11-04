package com.boutique.store;

import java.math.BigDecimal;
import java.util.List;

// Tout est public (le client doit savoir comment il peut payer) : aucun secret dans les paramètres
public record StoreSettingsResponse(String storeName, String supportEmail, boolean onlinePaymentEnabled,
		boolean codEnabled, BigDecimal codMaxAmount, BigDecimal codFee, List<String> codZones) {

	static StoreSettingsResponse from(StoreSettings settings) {
		return new StoreSettingsResponse(settings.getStoreName(), settings.getSupportEmail(),
				settings.isOnlinePaymentEnabled(), settings.isCodEnabled(), settings.getCodMaxAmount(),
				settings.getCodFee(), settings.getCodZones());
	}

}
