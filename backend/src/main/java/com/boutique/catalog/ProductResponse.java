package com.boutique.catalog;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description, BigDecimal price, int stock) {

	static ProductResponse from(Product product) {
		return new ProductResponse(product.id(), product.name(), product.description(), product.price(),
				product.stock());
	}

}
