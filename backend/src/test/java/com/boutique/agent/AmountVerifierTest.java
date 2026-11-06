package com.boutique.agent;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AmountVerifierTest {

	private final Set<BigDecimal> known = Set.of(new BigDecimal("8000.00"), new BigDecimal("125000"));

	@Test
	void amountsReadInDatabaseAreAccepted() {
		// Mêmes valeurs, écrites de toutes les façons qu'utilise un modèle (espace insécable, fine, point)
		assertThat(AmountVerifier.unknownAmounts(
				"Les sandales coûtent 8 000 FCFA, 8 000 F CFA, 8.000 francs ou 125 000 F.", known))
			.isEmpty();
	}

	@Test
	void inventedAmountIsReported() {
		assertThat(AmountVerifier.unknownAmounts("Prix spécial : 6 500 FCFA au lieu de 8000 FCFA", known))
			.containsExactly(new BigDecimal("6500"));
	}

	@Test
	void numbersWithoutCurrencyAreIgnored() {
		// Stock, quantité, numéro de commande : pas des montants
		assertThat(AmountVerifier.unknownAmounts("Il reste 9 unités, commande n° 1234, livraison en 3 jours.", known))
			.isEmpty();
	}

}
