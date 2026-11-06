package com.boutique.catalog;

import java.util.List;
import java.util.Locale;

import org.springframework.context.ApplicationEventPublisher;
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

	private final ApplicationEventPublisher events;

	public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
			ApplicationEventPublisher events) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.events = events;
	}

	public PageResponse<ProductResponse> findAll(String categorySlug, String search, boolean promotionsOnly,
			Pageable pageable) {
		Page<Product> products = productRepository
			.findAll(ProductSpecifications.matching(categorySlug, search, promotionsOnly), pageable);
		return PageResponse.from(products.map(ProductResponse::from));
	}

	public ProductResponse findById(Long id) {
		return ProductResponse.from(getProduct(id));
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		Product product = new Product(getCategory(request.categoryId()), request.name(), request.description(),
				request.price(), request.stock());
		String sku = normalizeSku(request.sku());
		if (sku != null && productRepository.existsBySku(sku)) {
			throw new DuplicateSkuException(sku);
		}
		product.updateMerchandising(blankToNull(request.brand()), sku, request.compareAtPrice());
		ProductResponse created = ProductResponse.from(productRepository.save(product));
		events.publishEvent(new ProductChangedEvent(created.id(), false));
		return created;
	}

	@Transactional
	public ProductResponse update(Long id, ProductRequest request) {
		Product product = getProduct(id);
		boolean wasOutOfStock = product.getStock() == 0;
		product.update(getCategory(request.categoryId()), request.name(), request.description(), request.price(),
				request.stock());
		if (wasOutOfStock && product.getStock() > 0) {
			events.publishEvent(new ProductBackInStockEvent(id));
		}
		String sku = normalizeSku(request.sku());
		if (sku != null && productRepository.existsBySkuAndIdNot(sku, id)) {
			throw new DuplicateSkuException(sku);
		}
		product.updateMerchandising(blankToNull(request.brand()), sku, request.compareAtPrice());
		events.publishEvent(new ProductChangedEvent(id, false));
		// Pas de save() : le produit est suivi par le contexte de persistance, l'UPDATE part au commit
		return ProductResponse.from(product);
	}

	// Les lignes des photos partent avec le produit (cascade) ; leurs fichiers seront effacés après le commit
	@Transactional
	public void delete(Long id) {
		Product product = getProduct(id);
		List<String> filenames = product.getImages().stream().map(ProductImage::getFilename).toList();
		productRepository.delete(product);
		events.publishEvent(new ProductImagesDeletedEvent(filenames));
		events.publishEvent(new ProductChangedEvent(id, true));
	}

	// Référence en majuscules, sans espaces autour ; vide = laissée au serveur
	private static String normalizeSku(String sku) {
		String value = blankToNull(sku);
		return (value == null) ? null : value.toUpperCase(Locale.ROOT);
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.strip();
	}

	private Product getProduct(Long id) {
		return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}

	private Category getCategory(Long id) {
		return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
	}

}
