package com.boutique.admin;

// Granularité du graphique des ventes : 30 derniers jours ou 12 derniers mois
public enum SalesPeriod {

	DAY("day"), MONTH("month");

	// Unité comprise par date_trunc de PostgreSQL
	final String sqlUnit;

	SalesPeriod(String sqlUnit) {
		this.sqlUnit = sqlUnit;
	}

}
