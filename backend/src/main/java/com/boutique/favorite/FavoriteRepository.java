package com.boutique.favorite;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FavoriteRepository extends JpaRepository<Favorite, Favorite.Key> {

	// Produits favoris avec leur catégorie en une requête (les photos suivent en une seule autre, @BatchSize)
	@Query("""
			select f from Favorite f join fetch f.product p join fetch p.category
			where f.id.userId = :userId order by f.createdAt desc
			""")
	List<Favorite> findWithProductsByUserId(Long userId);

	@Query("select f.id.productId from Favorite f where f.id.userId = :userId")
	List<Long> findProductIdsByUserId(Long userId);

	// Une seule requête DELETE, sans relire la ligne ; 0 ligne supprimée n'est pas une erreur (idempotent)
	@Modifying
	@Query("delete from Favorite f where f.id.userId = :userId and f.id.productId = :productId")
	int deleteByUserIdAndProductId(Long userId, Long productId);

}
