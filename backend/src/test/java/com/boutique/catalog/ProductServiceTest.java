package com.boutique.catalog;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

	private ProductRepository productRepository;

	private CategoryRepository categoryRepository;

	private ProductService productService;

	private final Category mode = new Category("Mode", "mode");

	@BeforeEach
	void setUp() {
		productRepository = mock(ProductRepository.class);
		categoryRepository = mock(CategoryRepository.class);
		productService = new ProductService(productRepository, categoryRepository, mock(ApplicationEventPublisher.class));
	}

	@Test
	void createRejectsUnknownCategory() {
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.create(request(99L, "Boubou bazin", "25000")))
			.isInstanceOf(CategoryNotFoundException.class);
		verify(productRepository, never()).save(any());
	}

	@Test
	void updateChangesManagedProductWithoutCallingSave() {
		Product product = new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 5);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(mode));

		ProductResponse response = productService.update(1L, request(1L, "Boubou bazin riche", "30000"));

		assertThat(response.name()).isEqualTo("Boubou bazin riche");
		assertThat(product.getPrice()).isEqualByComparingTo("30000");
		verify(productRepository, never()).save(any());
	}

	@Test
	void unknownProductThrowsNotFound() {
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.findById(99L)).isInstanceOf(ProductNotFoundException.class);
		assertThatThrownBy(() -> productService.delete(99L)).isInstanceOf(ProductNotFoundException.class);
	}

	private static ProductRequest request(Long categoryId, String name, String price) {
		return new ProductRequest(categoryId, name, null, new BigDecimal(price), 10, null, null, null);
	}

}
