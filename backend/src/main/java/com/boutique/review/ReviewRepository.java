package com.boutique.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	boolean existsByUserIdAndProductId(Long userId, Long productId);

	// L'auteur est chargé dans la même requête (son nom est affiché sous chaque avis)
	@EntityGraph(attributePaths = "user")
	Page<Review> findByProductId(Long productId, Pageable pageable);

	long countByProductId(Long productId);

	@Query("select avg(r.rating) from Review r where r.product.id = :productId")
	Double averageRating(Long productId);

}
