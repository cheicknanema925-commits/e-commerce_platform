package com.boutique.agent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Vérification par le CODE : chaque montant en FCFA cité dans la réponse doit correspondre à un montant lu en base
// pendant ce tour (prix, total…). Un petit modèle peut se tromper ou inventer : on le signale au lieu de le croire.
public final class AmountVerifier {

	// « 12 000 FCFA », « 12000 F CFA », « 12.000 francs », « 12 000 F » (espaces simples, insécables ou fines)
	private static final Pattern AMOUNT = Pattern
		.compile("(\\d{1,3}(?:[\\s\\u00A0\\u202F.]\\d{3})+|\\d+)\\s*(?:F\\s?CFA|FCFA|francs?|F\\b)", Pattern.CASE_INSENSITIVE);

	private AmountVerifier() {
	}

	public static List<BigDecimal> unknownAmounts(String answer, Set<BigDecimal> known) {
		List<BigDecimal> unknown = new ArrayList<>();
		Matcher matcher = AMOUNT.matcher(answer);
		while (matcher.find()) {
			BigDecimal amount = new BigDecimal(matcher.group(1).replaceAll("[\\s\\u00A0\\u202F.]", ""));
			if (known.stream().noneMatch((k) -> k.compareTo(amount) == 0)) {
				unknown.add(amount);
			}
		}
		return unknown;
	}

}
