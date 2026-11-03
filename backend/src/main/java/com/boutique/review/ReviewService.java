package com.boutique.review;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boutique.catalog.Product;
import com.boutique.catalog.ProductNotFoundException;
import com.boutique.catalog.ProductRepository;
import com.boutique.common.PageResponse;
import com.boutique.order.OrderItemRepository;
import com.boutique.order.OrderStatus;
import com.boutique.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class ReviewService {

	private final ReviewRepository reviewRepository;

	private final ProductRepository productRepository;

	private final OrderItemRepository orderItemRepository;

	private final UserRepository userRepository;

	public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
			OrderItemRepository orderItemRepository, UserRepository userRepository) {
		this.reviewRepository = reviewRepository;
		this.productRepository = productRepository;
		this.orderItemRepository = orderItemRepository;
		this.userRepository = userRepository;
	}

	// Règles : avoir acheté ET reçu le produit (commande DELIVERED), et ne le noter qu'une seule fois
	@Transactional
	public ReviewResponse create(Long userId, Long productId, ReviewRequest request) {
		Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
		if (!orderItemRepository.existsByOrderUserIdAndProductIdAndOrderStatus(userId, productId, OrderStatus.DELIVERED)) {
			throw new ReviewNotAllowedException();
		}
		// Vérification lisible ici ; la contrainte UNIQUE (user_id, product_id) de la base garantit la règle
		// même si deux requêtes arrivent en même temps
		if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
			throw new DuplicateReviewException();
		}
		String comment = StringUtils.hasText(request.comment()) ? request.comment().strip() : null;
		Review review = new Review(userRepository.getReferenceById(userId), product, request.rating(), comment);
		return ReviewResponse.from(reviewRepository.save(review));
	}

	public PageResponse<ReviewResponse> findByProduct(Long productId, Pageable pageable) {
		return PageResponse.from(reviewRepository.findByProductId(productId, pageable).map(ReviewResponse::from));
	}

	public RatingResponse rating(Long productId) {
		Double average = reviewRepository.averageRating(productId);
		double rounded = (average == null) ? 0 : Math.round(average * 10) / 10.0;
		return new RatingResponse(rounded, reviewRepository.countByProductId(productId));
	}

}
