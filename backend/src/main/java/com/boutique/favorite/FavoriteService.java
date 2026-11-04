package com.boutique.favorite;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.catalog.Product;
import com.boutique.catalog.ProductNotFoundException;
import com.boutique.catalog.ProductRepository;
import com.boutique.catalog.ProductResponse;

@Service
@Transactional(readOnly = true)
public class FavoriteService {

	private final FavoriteRepository favoriteRepository;

	private final ProductRepository productRepository;

	public FavoriteService(FavoriteRepository favoriteRepository, ProductRepository productRepository) {
		this.favoriteRepository = favoriteRepository;
		this.productRepository = productRepository;
	}

	// Conversion en DTO dans la transaction : catégorie et photos sont lues ici (open-in-view désactivé)
	public List<ProductResponse> favorites(Long userId) {
		return favoriteRepository.findWithProductsByUserId(userId)
			.stream()
			.map((favorite) -> ProductResponse.from(favorite.getProduct()))
			.toList();
	}

	public List<Long> favoriteProductIds(Long userId) {
		return favoriteRepository.findProductIdsByUserId(userId);
	}

	// Idempotent : ajouter deux fois le même favori ne crée pas de doublon et ne renvoie pas d'erreur
	@Transactional
	public void add(Long userId, Long productId) {
		if (favoriteRepository.existsById(new Favorite.Key(userId, productId))) {
			return;
		}
		Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
		favoriteRepository.save(new Favorite(userId, product));
	}

	@Transactional
	public void remove(Long userId, Long productId) {
		favoriteRepository.deleteByUserIdAndProductId(userId, productId);
	}

}
