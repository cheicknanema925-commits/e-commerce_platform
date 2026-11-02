package com.boutique.catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class ProductService {

	private final Map<Long, Product> products = new ConcurrentHashMap<>();

	private final AtomicLong nextId = new AtomicLong();

	public List<ProductResponse> findAll() {
		return products.values()
			.stream()
			.sorted(Comparator.comparing(Product::id))
			.map(ProductResponse::from)
			.toList();
	}

	public ProductResponse findById(Long id) {
		Product product = products.get(id);
		if (product == null) {
			throw new ProductNotFoundException(id);
		}
		return ProductResponse.from(product);
	}

	public ProductResponse create(ProductRequest request) {
		long id = nextId.incrementAndGet();
		Product product = toProduct(id, request);
		products.put(id, product);
		return ProductResponse.from(product);
	}

	public ProductResponse update(Long id, ProductRequest request) {
		// computeIfPresent : lecture + remplacement en une seule opération, sans qu'un autre thread s'intercale
		Product updated = products.computeIfPresent(id, (key, existing) -> toProduct(key, request));
		if (updated == null) {
			throw new ProductNotFoundException(id);
		}
		return ProductResponse.from(updated);
	}

	public void delete(Long id) {
		if (products.remove(id) == null) {
			throw new ProductNotFoundException(id);
		}
	}

	private static Product toProduct(Long id, ProductRequest request) {
		return new Product(id, request.name(), request.description(), request.price(), request.stock());
	}

}
