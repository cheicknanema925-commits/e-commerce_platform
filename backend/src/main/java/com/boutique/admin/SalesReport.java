package com.boutique.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Une entrée par jour (ou par mois), y compris les périodes sans vente (à zéro) : le graphique n'a pas de trou
public record SalesReport(SalesPeriod period, List<Point> points) {

	// start : premier jour de la période (le 1er du mois pour MONTH)
	public record Point(LocalDate start, BigDecimal collected, long orders) {
	}

}
