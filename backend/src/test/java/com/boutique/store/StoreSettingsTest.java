package com.boutique.store;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.boutique.store.StoreSettings.PaymentUnavailableException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Règles de paiement des paramètres : test unitaire pur, sans Spring ni base
class StoreSettingsTest {

	@Test
	void cashZonesIgnoreAccentsAndCase() {
		StoreSettings settings = settings(true, null, List.of("Dakar", " Guédiawaye "));

		assertThatCode(() -> settings.checkCashOnDeliveryAllowed(BigDecimal.TEN, "GUEDIAWAYE")).doesNotThrowAnyException();
		assertThatThrownBy(() -> settings.checkCashOnDeliveryAllowed(BigDecimal.TEN, "Thiès"))
			.isInstanceOf(PaymentUnavailableException.class)
			.hasMessageContaining("Villes desservies : Dakar, Guédiawaye");
	}

	@Test
	void cashAboveTheCeilingIsRefused() {
		StoreSettings settings = settings(true, new BigDecimal("200000"), List.of());

		assertThatCode(() -> settings.checkCashOnDeliveryAllowed(new BigDecimal("200000"), "Dakar")).doesNotThrowAnyException();
		assertThatThrownBy(() -> settings.checkCashOnDeliveryAllowed(new BigDecimal("200001"), "Dakar"))
			.hasMessageContaining("Au-delà de 200000 FCFA");
	}

	@Test
	void disabledCashIsRefusedEverywhere() {
		assertThatThrownBy(() -> settings(false, null, List.of()).checkCashOnDeliveryAllowed(BigDecimal.TEN, "Dakar"))
			.isInstanceOf(PaymentUnavailableException.class);
	}

	private static StoreSettings settings(boolean codEnabled, BigDecimal max, List<String> zones) {
		StoreSettings settings = new StoreSettings();
		settings.update("Shop Africa", "contact@shopafrica.sn", true, codEnabled, max, BigDecimal.ZERO, zones);
		return settings;
	}

}
