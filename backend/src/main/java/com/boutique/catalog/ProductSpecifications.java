package com.boutique.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

// Filtres facultatifs du catalogue, combinés en une seule requête (Criteria API : paramètres liés, pas d'injection SQL)
final class ProductSpecifications {

	private ProductSpecifications() {
	}

	static Specification<Product> matching(String categorySlug, String search) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (StringUtils.hasText(categorySlug)) {
				predicates.add(cb.equal(root.get("category").get("slug"), categorySlug));
			}
			if (StringUtils.hasText(search)) {
				String pattern = "%" + escapeLike(search.strip().toLowerCase(Locale.ROOT)) + "%";
				predicates.add(cb.like(cb.lower(root.get("name")), pattern, '\\'));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	// « % » et « _ » sont des jokers dans LIKE : saisis par l'utilisateur, ils doivent être cherchés tels quels
	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
