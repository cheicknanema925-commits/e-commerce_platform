package com.boutique.catalog;

import java.math.BigDecimal;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.boutique.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

// Tranche JPA uniquement (entités, repositories, Flyway) sur un vrai PostgreSQL.
// Chaque test est annulé (rollback) à la fin : aucune donnée ne reste en base.
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@BeforeEach
	void setUp() {
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		Category beaute = categoryRepository.findBySlug("beaute").orElseThrow();
		entityManager.persist(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 5));
		entityManager.persist(new Product(mode, "Sandales cuir", null, new BigDecimal("8000"), 12));
		entityManager.persist(new Product(mode, "Pagne wax", null, new BigDecimal("12000"), 30));
		entityManager.persist(new Product(beaute, "Beurre de karité", null, new BigDecimal("3000"), 50));
		// Envoie les INSERT puis vide le contexte de persistance : les lectures suivantes viendront de la base
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void findByCategorySlugReturnsOnlyThatCategoryWithPagination() {
		Page<Product> page = productRepository.findByCategorySlug("mode", PageRequest.of(0, 2, Sort.by("price")));

		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getContent()).extracting(Product::getName).containsExactly("Sandales cuir", "Pagne wax");
	}

	@Test
	void findAllLoadsCategoryWithEntityGraph() {
		Page<Product> page = productRepository.findAll(PageRequest.of(0, 10));

		assertThat(page.getContent()).allSatisfy((product) -> assertThat(Hibernate.isInitialized(product.getCategory())).isTrue());
	}

	@Test
	void findByIdLeavesCategoryLazy() {
		Long id = productRepository.findAll(PageRequest.of(0, 1)).getContent().get(0).getId();
		entityManager.clear();

		Product product = productRepository.findById(id).orElseThrow();

		// Sans @EntityGraph, la catégorie n'est qu'un « proxy » vide, chargé seulement si on y accède
		assertThat(Hibernate.isInitialized(product.getCategory())).isFalse();
	}

	@Test
	void categoriesAreSortedByName() {
		assertThat(categoryRepository.findAllByOrderByNameAsc()).extracting(Category::getSlug)
			.containsExactly("alimentation", "beaute", "electronique", "maison", "mode");
	}

}
