package com.boutique.catalog;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

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

	long countByStock(int stock);

	// Valeur du stock au prix de vente actuel
	@Query("select coalesce(sum(p.price * p.stock), 0) from Product p")
	BigDecimal stockValue();

	// Requête dérivée : Spring Data lit le nom de la méthode et écrit le SQL (... WHERE category.slug = ?)
	@EntityGraph(attributePaths = "category")
	Page<Product> findByCategorySlug(String slug, Pageable pageable);

}
