package com.boutique.catalog;

import java.math.BigDecimal;
import java.util.List;

// images : la première est la photo principale (liste vide = pas encore de photo).
// compareAtPrice / discountPercent : prix barré et remise calculée (null sans promotion)
public record ProductResponse(Long id, String name, String description, BigDecimal price, int stock, Long categoryId,
		String categoryName, List<ProductImageResponse> images, String brand, String sku, BigDecimal compareAtPrice,
		Integer discountPercent) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
				product.getStock(), product.getCategory().getId(), product.getCategory().getName(),
				product.getImages().stream().map(ProductImageResponse::from).toList(), product.getBrand(),
				product.getSku(), product.getCompareAtPrice(), product.getDiscountPercent());
	}

}
