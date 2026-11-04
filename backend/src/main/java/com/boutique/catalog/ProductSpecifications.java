package com.boutique.catalog;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.boutique.common.LikePatterns;

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
				predicates.add(cb.like(cb.lower(root.get("name")), LikePatterns.contains(search), LikePatterns.ESCAPE));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

}
