package com.boutique.review;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.common.PageResponse;
import com.boutique.user.CurrentUserResolver;

// Lecture publique (GET /api/products/** est ouvert), écriture réservée aux clients connectés
@RestController
@RequestMapping("/api/products/{productId}")
public class ReviewController {

	private final ReviewService reviewService;

	private final CurrentUserResolver currentUserResolver;

	public ReviewController(ReviewService reviewService, CurrentUserResolver currentUserResolver) {
		this.reviewService = reviewService;
		this.currentUserResolver = currentUserResolver;
	}

	@PostMapping("/reviews")
	@ResponseStatus(HttpStatus.CREATED)
	public ReviewResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId,
			@Valid @RequestBody ReviewRequest request) {
		return reviewService.create(currentUserResolver.userId(jwt), productId, request);
	}

	@GetMapping("/reviews")
	public PageResponse<ReviewResponse> findAll(@PathVariable Long productId,
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return reviewService.findByProduct(productId, pageable);
	}

	@GetMapping("/rating")
	public RatingResponse rating(@PathVariable Long productId) {
		return reviewService.rating(productId);
	}

}
