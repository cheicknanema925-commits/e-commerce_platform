package com.boutique.catalog;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductServiceTest {

	private ProductService productService;

	@BeforeEach
	void setUp() {
		productService = new ProductService();
	}

	@Test
	void createAssignsIncrementingIds() {
		ProductResponse first = productService.create(request("Boubou bazin", "25000"));
		ProductResponse second = productService.create(request("Sandales cuir", "8000"));

		assertThat(first.id()).isEqualTo(1L);
		assertThat(second.id()).isEqualTo(2L);
		assertThat(productService.findAll()).extracting(ProductResponse::name)
			.containsExactly("Boubou bazin", "Sandales cuir");
	}

	@Test
	void updateReplacesExistingProduct() {
		Long id = productService.create(request("Boubou bazin", "25000")).id();

		productService.update(id, request("Boubou bazin riche", "30000"));

		ProductResponse updated = productService.findById(id);
		assertThat(updated.name()).isEqualTo("Boubou bazin riche");
		assertThat(updated.price()).isEqualByComparingTo("30000");
	}

	@Test
	void deleteRemovesProduct() {
		Long id = productService.create(request("Boubou bazin", "25000")).id();

		productService.delete(id);

		assertThat(productService.findAll()).isEmpty();
	}

	@Test
	void unknownIdThrowsNotFound() {
		assertThatThrownBy(() -> productService.findById(99L)).isInstanceOf(ProductNotFoundException.class);
		assertThatThrownBy(() -> productService.update(99L, request("X", "1")))
			.isInstanceOf(ProductNotFoundException.class);
		assertThatThrownBy(() -> productService.delete(99L)).isInstanceOf(ProductNotFoundException.class);
	}

	private static ProductRequest request(String name, String price) {
		return new ProductRequest(name, null, new BigDecimal(price), 10);
	}

}
