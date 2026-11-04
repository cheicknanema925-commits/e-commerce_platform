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

	// search : nom, marque ou référence ; promotionsOnly : seulement les produits avec un prix barré
	static Specification<Product> matching(String categorySlug, String search, boolean promotionsOnly) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (StringUtils.hasText(categorySlug)) {
				predicates.add(cb.equal(root.get("category").get("slug"), categorySlug));
			}
			if (StringUtils.hasText(search)) {
				String pattern = LikePatterns.contains(search);
				predicates.add(cb.or(cb.like(cb.lower(root.get("name")), pattern, LikePatterns.ESCAPE),
						cb.like(cb.lower(root.get("brand")), pattern, LikePatterns.ESCAPE),
						cb.like(cb.lower(root.get("sku")), pattern, LikePatterns.ESCAPE)));
			}
			if (promotionsOnly) {
				predicates.add(cb.isNotNull(root.get("compareAtPrice")));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

}
