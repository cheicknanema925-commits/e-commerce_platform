package com.boutique.order;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.boutique.common.LikePatterns;
import com.boutique.user.User;

// Filtres facultatifs des commandes du back-office : statut, mode de paiement, recherche
// (« 42 » ou « #42 » = numéro de commande ; sinon nom ou email du client)
final class OrderSpecifications {

	private OrderSpecifications() {
	}

	static Specification<Order> matching(OrderStatus status, PaymentMethod paymentMethod, String search) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			if (paymentMethod != null) {
				predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
			}
			if (StringUtils.hasText(search)) {
				String term = search.strip().replaceFirst("^#", "");
				if (term.matches("\\d{1,18}")) {
					predicates.add(cb.equal(root.get("id"), Long.valueOf(term)));
				}
				else {
					Join<Order, User> user = root.join("user");
					String pattern = LikePatterns.contains(term);
					predicates.add(cb.or(cb.like(cb.lower(user.get("email")), pattern, LikePatterns.ESCAPE),
							cb.like(cb.lower(user.get("fullName")), pattern, LikePatterns.ESCAPE)));
				}
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

}
