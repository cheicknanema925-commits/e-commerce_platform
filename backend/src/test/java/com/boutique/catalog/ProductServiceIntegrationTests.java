package com.boutique.catalog;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.boutique.TestcontainersConfiguration;
import com.boutique.common.PageResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ProductServiceIntegrationTests {

	@Autowired
	private ProductService productService;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@AfterEach
	void cleanUp() {
		productRepository.deleteAll();
	}

	@Test
	void flywayCreatedReferenceCategories() {
		assertThat(categoryRepository.findAll()).extracting(Category::getSlug)
			.contains("mode", "beaute", "electronique", "maison", "alimentation");
	}

	@Test
	void updateIsSavedWithoutCallingSave() {
		Long modeId = categoryId("mode");
		Long id = productService.create(request(modeId, "Boubou bazin", "25000")).id();

		productService.update(id, request(modeId, "Boubou bazin riche", "30000"));

		// Relu depuis la base dans une nouvelle transaction : l'UPDATE a bien été envoyé au commit
		Product reloaded = productRepository.findById(id).orElseThrow();
		assertThat(reloaded.getName()).isEqualTo("Boubou bazin riche");
		assertThat(reloaded.getPrice()).isEqualByComparingTo("30000");
	}

	@Test
	void findAllFiltersByCategoryAndPaginates() {
		Long modeId = categoryId("mode");
		productService.create(request(modeId, "Boubou bazin", "25000"));
		productService.create(request(modeId, "Sandales cuir", "8000"));
		productService.create(request(modeId, "Pagne wax", "12000"));
		productService.create(request(categoryId("beaute"), "Beurre de karité", "3000"));

		PageResponse<ProductResponse> page = productService.findAll("mode", null, PageRequest.of(0, 2, Sort.by("price")));

		assertThat(page.totalElements()).isEqualTo(3);
		assertThat(page.totalPages()).isEqualTo(2);
		assertThat(page.content()).extracting(ProductResponse::name).containsExactly("Sandales cuir", "Pagne wax");
	}

	@Test
	void searchIsCaseInsensitiveAndTreatsWildcardsLiterally() {
		Long modeId = categoryId("mode");
		productService.create(request(modeId, "Boubou bazin", "25000"));
		productService.create(request(modeId, "Pagne wax", "12000"));

		assertThat(productService.findAll(null, "BAZIN", PageRequest.of(0, 10)).content()).extracting(ProductResponse::name)
			.containsExactly("Boubou bazin");
		// « % » est cherché comme un caractère, pas comme un joker qui renverrait tout le catalogue
		assertThat(productService.findAll(null, "%", PageRequest.of(0, 10)).content()).isEmpty();
	}

	private Long categoryId(String slug) {
		return categoryRepository.findBySlug(slug).orElseThrow().getId();
	}

	private static ProductRequest request(Long categoryId, String name, String price) {
		return new ProductRequest(categoryId, name, null, new BigDecimal(price), 10);
	}

}
