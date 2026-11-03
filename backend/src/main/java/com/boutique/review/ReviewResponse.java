package com.boutique.review;

import java.time.Instant;

// L'auteur est affiché « Prénom N. » : jamais d'email ni de nom complet dans une réponse publique
public record ReviewResponse(Long id, int rating, String comment, String author, Instant createdAt) {

	static ReviewResponse from(Review review) {
		return new ReviewResponse(review.getId(), review.getRating(), review.getComment(),
				displayName(review.getUser().getFullName()), review.getCreatedAt());
	}

	private static String displayName(String fullName) {
		String[] parts = fullName.strip().split("\\s+");
		return (parts.length == 1) ? parts[0] : parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
	}

}
