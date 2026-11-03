package com.boutique.catalog;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	// Recherche avec filtres facultatifs (catégorie, texte) ; la catégorie est chargée dans la même requête
	@Override
	@EntityGraph(attributePaths = "category")
	Page<Product> findAll(Specification<Product> specification, Pageable pageable);

	// @EntityGraph : la catégorie est chargée dans la même requête (JOIN), pas une requête par produit
	@Override
	@EntityGraph(attributePaths = "category")
	Page<Product> findAll(Pageable pageable);

	// Produits bientôt en rupture (requête dérivée : « Top10 » limite, « LessThanEqual » compare, « OrderBy » trie)
	List<Product> findTop10ByStockLessThanEqualOrderByStockAsc(int stock);

	// Requête dérivée : Spring Data lit le nom de la méthode et écrit le SQL (... WHERE category.slug = ?)
	@EntityGraph(attributePaths = "category")
	Page<Product> findByCategorySlug(String slug, Pageable pageable);

}
