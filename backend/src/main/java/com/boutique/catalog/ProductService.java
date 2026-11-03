package com.boutique.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.common.PageResponse;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;

	private final CategoryRepository categoryRepository;

	public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
	}

	public PageResponse<ProductResponse> findAll(String categorySlug, String search, Pageable pageable) {
		Page<Product> products = productRepository.findAll(ProductSpecifications.matching(categorySlug, search),
				pageable);
		return PageResponse.from(products.map(ProductResponse::from));
	}

	public ProductResponse findById(Long id) {
		return ProductResponse.from(getProduct(id));
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		Product product = new Product(getCategory(request.categoryId()), request.name(), request.description(),
				request.price(), request.stock());
		return ProductResponse.from(productRepository.save(product));
	}

	@Transactional
	public ProductResponse update(Long id, ProductRequest request) {
		Product product = getProduct(id);
		product.update(getCategory(request.categoryId()), request.name(), request.description(), request.price(),
				request.stock());
		// Pas de save() : le produit est suivi par le contexte de persistance, l'UPDATE part au commit
		return ProductResponse.from(product);
	}

	@Transactional
	public void delete(Long id) {
		productRepository.delete(getProduct(id));
	}

	private Product getProduct(Long id) {
		return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}

	private Category getCategory(Long id) {
		return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
	}

}
