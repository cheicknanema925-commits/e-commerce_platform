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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

		PageResponse<ProductResponse> page = productService.findAll("mode", null, false, PageRequest.of(0, 2, Sort.by("price")));

		assertThat(page.totalElements()).isEqualTo(3);
		assertThat(page.totalPages()).isEqualTo(2);
		assertThat(page.content()).extracting(ProductResponse::name).containsExactly("Sandales cuir", "Pagne wax");
	}

	@Test
	void searchIsCaseInsensitiveAndTreatsWildcardsLiterally() {
		Long modeId = categoryId("mode");
		productService.create(request(modeId, "Boubou bazin", "25000"));
		productService.create(request(modeId, "Pagne wax", "12000"));

		assertThat(productService.findAll(null, "BAZIN", false, PageRequest.of(0, 10)).content()).extracting(ProductResponse::name)
			.containsExactly("Boubou bazin");
		// « % » est cherché comme un caractère, pas comme un joker qui renverrait tout le catalogue
		assertThat(productService.findAll(null, "%", false, PageRequest.of(0, 10)).content()).isEmpty();
	}

	@Test
	void skuIsGeneratedWhenMissingAndMustBeUnique() {
		Long modeId = categoryId("mode");
		ProductResponse generated = productService.create(request(modeId, "Boubou bazin", "25000"));
		ProductResponse chosen = productService
			.create(new ProductRequest(modeId, "Pagne wax", null, new BigDecimal("12000"), 5, "Vlisco", " wax-01 ", null));

		assertThat(generated.sku()).matches("SHA-[0-9A-F]{8}");
		// Référence saisie : espaces retirés, mise en majuscules
		assertThat(chosen.sku()).isEqualTo("WAX-01");
		assertThat(chosen.brand()).isEqualTo("Vlisco");
		assertThatThrownBy(() -> productService
			.create(new ProductRequest(modeId, "Autre pagne", null, new BigDecimal("9000"), 5, null, "WAX-01", null)))
			.isInstanceOf(DuplicateSkuException.class);
	}

	@Test
	void promotionShowsTheDiscountAndCanBeFiltered() {
		Long modeId = categoryId("mode");
		productService
			.create(new ProductRequest(modeId, "Sandales cuir", null, new BigDecimal("8000"), 5, null, null, new BigDecimal("10000")));
		productService.create(request(modeId, "Pagne wax", "12000"));

		PageResponse<ProductResponse> promotions = productService.findAll(null, null, true, PageRequest.of(0, 10));

		assertThat(promotions.content()).extracting(ProductResponse::name).containsExactly("Sandales cuir");
		assertThat(promotions.content().getFirst().discountPercent()).isEqualTo(20);
	}

	// Prix barré inférieur ou égal au prix de vente : refusé par le code (message clair) avant la contrainte en base
	@Test
	void compareAtPriceMustBeHigherThanThePrice() {
		Long modeId = categoryId("mode");
		assertThatThrownBy(() -> productService
			.create(new ProductRequest(modeId, "Pagne wax", null, new BigDecimal("12000"), 5, null, null, new BigDecimal("12000"))))
			.isInstanceOf(InvalidPromotionException.class);
	}

	@Test
	void searchAlsoLooksAtBrandAndSku() {
		Long modeId = categoryId("mode");
		productService.create(new ProductRequest(modeId, "Pagne", null, new BigDecimal("12000"), 5, "Vlisco", "PAG-77", null));

		assertThat(productService.findAll(null, "vlisco", false, PageRequest.of(0, 10)).content()).hasSize(1);
		assertThat(productService.findAll(null, "pag-77", false, PageRequest.of(0, 10)).content()).hasSize(1);
	}

	private Long categoryId(String slug) {
		return categoryRepository.findBySlug(slug).orElseThrow().getId();
	}

	private static ProductRequest request(Long categoryId, String name, String price) {
		return new ProductRequest(categoryId, name, null, new BigDecimal(price), 10, null, null, null);
	}

}
