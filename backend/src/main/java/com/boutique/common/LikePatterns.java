package com.boutique.common;

import java.util.Locale;

// Texte tapé par un utilisateur → motif LIKE « contient », insensible à la casse.
// « % » et « _ » sont des jokers dans LIKE : saisis par l'utilisateur, ils doivent être cherchés tels quels.
public final class LikePatterns {

	public static final char ESCAPE = '\\';

	private LikePatterns() {
	}

	public static String contains(String text) {
		String escaped = text.strip()
			.toLowerCase(Locale.ROOT)
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_");
		return "%" + escaped + "%";
	}

}
