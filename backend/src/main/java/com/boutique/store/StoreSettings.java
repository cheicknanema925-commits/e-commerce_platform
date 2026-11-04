package com.boutique.store;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UpdateTimestamp;

import com.boutique.common.ConflictException;

// Paramètres de la boutique (une seule ligne, id = 1). Les règles de paiement sont vérifiées ICI, par le serveur :
// masquer un bouton dans le navigateur ne suffit jamais.
@Entity
@Table(name = "store_settings")
public class StoreSettings {

	static final short SINGLE_ROW_ID = 1;

	@Id
	private Short id;

	private String storeName;

	private String supportEmail;

	private boolean onlinePaymentEnabled;

	private boolean codEnabled;

	private BigDecimal codMaxAmount;

	private BigDecimal codFee;

	private String codZones;

	@UpdateTimestamp
	private Instant updatedAt;

	protected StoreSettings() {
	}

	void update(String storeName, String supportEmail, boolean onlinePaymentEnabled, boolean codEnabled,
			BigDecimal codMaxAmount, BigDecimal codFee, List<String> codZones) {
		this.storeName = storeName;
		this.supportEmail = supportEmail;
		this.onlinePaymentEnabled = onlinePaymentEnabled;
		this.codEnabled = codEnabled;
		this.codMaxAmount = codMaxAmount;
		this.codFee = codFee;
		this.codZones = String.join(", ", codZones.stream().map(String::strip).filter((z) -> !z.isEmpty()).toList());
	}

	public void checkOnlinePaymentAllowed() {
		if (!onlinePaymentEnabled) {
			throw new PaymentUnavailableException("Le paiement en ligne est momentanément indisponible. Choisissez le paiement à la livraison.");
		}
	}

	// Espèces : activé, sous le plafond, et dans une zone desservie (comparaison sans accents ni majuscules)
	public void checkCashOnDeliveryAllowed(BigDecimal amount, String city) {
		if (!codEnabled) {
			throw new PaymentUnavailableException("Le paiement à la livraison est momentanément indisponible. Choisissez le paiement en ligne.");
		}
		if (codMaxAmount != null && amount.compareTo(codMaxAmount) > 0) {
			throw new PaymentUnavailableException("Au-delà de " + codMaxAmount.toBigInteger()
					+ " FCFA, le paiement à la livraison n'est pas possible : payez en ligne.");
		}
		List<String> zones = getCodZones();
		if (!zones.isEmpty() && zones.stream().noneMatch((zone) -> normalize(zone).equals(normalize(city)))) {
			throw new PaymentUnavailableException("Le paiement à la livraison n'est pas proposé à " + city.strip()
					+ ". Villes desservies : " + String.join(", ", zones) + ".");
		}
	}

	// « Guédiawaye » = « guediawaye » = « GUEDIAWAYE »
	static String normalize(String text) {
		return Normalizer.normalize(text.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
	}

	public String getStoreName() {
		return storeName;
	}

	public String getSupportEmail() {
		return supportEmail;
	}

	public boolean isOnlinePaymentEnabled() {
		return onlinePaymentEnabled;
	}

	public boolean isCodEnabled() {
		return codEnabled;
	}

	public BigDecimal getCodMaxAmount() {
		return codMaxAmount;
	}

	public BigDecimal getCodFee() {
		return codFee;
	}

	public List<String> getCodZones() {
		return codZones.isBlank() ? List.of() : Arrays.stream(codZones.split(",")).map(String::strip).filter((z) -> !z.isEmpty()).toList();
	}

	public static class PaymentUnavailableException extends ConflictException {

		PaymentUnavailableException(String message) {
			super(message);
		}

	}

}
