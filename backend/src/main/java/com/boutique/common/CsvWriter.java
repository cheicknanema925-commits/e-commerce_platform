package com.boutique.common;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

// Fichier CSV lisible par Excel en français : séparateur « ; », fins de ligne CRLF, marqueur UTF-8 (BOM) pour les accents.
// Protégé contre l'injection de formule : une cellule qui commence par = + - @ (ex. =HYPERLINK(...) dans un nom de
// client) serait exécutée par le tableur ; on la préfixe d'une apostrophe pour qu'elle reste du texte.
public final class CsvWriter {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Dakar"));

	private final StringBuilder content = new StringBuilder("﻿");

	public CsvWriter row(List<?> values) {
		content.append(String.join(";", values.stream().map(CsvWriter::cell).toList())).append("\r\n");
		return this;
	}

	public byte[] toBytes() {
		return content.toString().getBytes(StandardCharsets.UTF_8);
	}

	static String cell(Object value) {
		String text = switch (value) {
			case null -> "";
			case BigDecimal amount -> amount.stripTrailingZeros().toPlainString();
			case Instant instant -> DATE.format(instant);
			default -> Objects.toString(value);
		};
		if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
			text = "'" + text;
		}
		if (text.contains(";") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
			text = "\"" + text.replace("\"", "\"\"") + "\"";
		}
		return text;
	}

}
