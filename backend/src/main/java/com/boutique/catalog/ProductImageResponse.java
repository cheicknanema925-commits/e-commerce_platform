package com.boutique.catalog;

// url relative (/api/images/…) : servie par la même origine que l'API, derrière le proxy Vite ou Caddy
public record ProductImageResponse(Long id, String url) {

	static ProductImageResponse from(ProductImage image) {
		return new ProductImageResponse(image.getId(), ImageController.PATH + image.getFilename());
	}

}
