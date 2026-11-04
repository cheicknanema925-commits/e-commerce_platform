package com.boutique.common;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

	@Test
	void formulaInjectionIsNeutralized() {
		assertThat(CsvWriter.cell("=HYPERLINK(\"http://pirate.example\")")).isEqualTo("\"'=HYPERLINK(\"\"http://pirate.example\"\")\"");
		assertThat(CsvWriter.cell("+221 77 123 45 67")).isEqualTo("'+221 77 123 45 67");
		assertThat(CsvWriter.cell("@SOMME(A1)")).isEqualTo("'@SOMME(A1)");
		assertThat(CsvWriter.cell("Awa Diop")).isEqualTo("Awa Diop");
	}

	@Test
	void separatorsQuotesAndAmountsAreWrittenSafely() {
		assertThat(CsvWriter.cell("Boubou; bazin")).isEqualTo("\"Boubou; bazin\"");
		assertThat(CsvWriter.cell(new BigDecimal("25000.00"))).isEqualTo("25000");
		assertThat(CsvWriter.cell(null)).isEmpty();
	}

	// Marqueur UTF-8 (BOM) au début : Excel affiche correctement les accents
	@Test
	void fileStartsWithTheUtf8MarkerAndUsesWindowsLineEndings() {
		String csv = new String(new CsvWriter().row(List.of("Catégorie", "Prix")).toBytes(), StandardCharsets.UTF_8);

		assertThat(csv).isEqualTo("﻿Catégorie;Prix\r\n");
	}

}
