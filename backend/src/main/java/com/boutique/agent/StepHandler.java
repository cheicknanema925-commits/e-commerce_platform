package com.boutique.agent;

import java.util.Map;

// Exécute un type d'étape confirmée (stratégie). Chaque gestionnaire appelle un service métier existant :
// mêmes règles que l'API (stock, droits, limites), aucun raccourci pour l'IA.
public interface StepHandler {

	String type();

	// Agent autorisé à proposer ce type d'étape : une étape ADMIN ne peut jamais être exécutée pour un client
	AgentKind agent();

	// Renvoie un compte rendu lisible ; une exception signale l'échec de CETTE étape
	String execute(Long userId, Map<String, Object> params);

	static long longParam(Map<String, Object> params, String name) {
		if (!(params.get(name) instanceof Number number)) {
			throw new IllegalArgumentException("Paramètre manquant : " + name);
		}
		return number.longValue();
	}

	static String stringParam(Map<String, Object> params, String name) {
		if (!(params.get(name) instanceof String value)) {
			throw new IllegalArgumentException("Paramètre manquant : " + name);
		}
		return value;
	}

	static Long optionalLongParam(Map<String, Object> params, String name) {
		return (params.get(name) instanceof Number number) ? number.longValue() : null;
	}

}
