package com.boutique.catalog;

public record CategoryResponse(Long id, String name, String slug) {

	static CategoryResponse from(Category category) {
		return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
	}

}
