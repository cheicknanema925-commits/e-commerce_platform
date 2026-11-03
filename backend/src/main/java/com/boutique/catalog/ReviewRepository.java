package com.boutique.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	boolean existsByUserIdAndProductId(Long userId, Long productId);

}
