package com.boutique.agent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.boutique.catalog.ProductChangedEvent;
import com.boutique.catalog.ProductNotFoundException;
import com.boutique.catalog.ProductResponse;
import com.boutique.catalog.ProductService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductIndexerTest {

	private VectorStore vectorStore;

	private ProductService productService;

	@BeforeEach
	void setUp() {
		vectorStore = mock(VectorStore.class);
		productService = mock(ProductService.class);
	}

	@Test
	void disabledIndexIsNeverTouched() {
		ProductIndexer indexer = new ProductIndexer(vectorStore, productService, false);

		indexer.indexAllOnStartup();
		indexer.onProductChanged(new ProductChangedEvent(1L, false));

		assertThat(indexer.similarProducts("sandales", 5)).isEmpty();
		verifyNoInteractions(vectorStore, productService);
	}

	@Test
	void similarProductsAreReadAgainFromDatabase() {
		ProductIndexer indexer = new ProductIndexer(vectorStore, productService, true);
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(hit(2L), hit(7L)));
		when(productService.findById(2L)).thenReturn(product(2L, "Sandales cuir"));
		// Supprimé après l'indexation : ignoré au lieu de faire échouer la recherche
		when(productService.findById(7L)).thenThrow(new ProductNotFoundException(7L));

		assertThat(indexer.similarProducts("sandalles", 5)).extracting(ProductResponse::name)
			.containsExactly("Sandales cuir");
	}

	@Test
	void unavailableEmbeddingModelDoesNotBreakSearchNorEvents() {
		ProductIndexer indexer = new ProductIndexer(vectorStore, productService, true);
		when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new IllegalStateException("Ollama arrêté"));
		when(productService.findById(2L)).thenReturn(product(2L, "Sandales cuir"));
		doThrow(new IllegalStateException("Ollama arrêté")).when(vectorStore).add(any());

		assertThat(indexer.similarProducts("sandales", 5)).isEmpty();
		indexer.onProductChanged(new ProductChangedEvent(2L, false));
	}

	@Test
	void deletedProductIsRemovedFromIndex() {
		new ProductIndexer(vectorStore, productService, true).onProductChanged(new ProductChangedEvent(3L, true));

		verify(vectorStore).delete(List.of("product-3"));
	}

	private static Document hit(Long productId) {
		return Document.builder().id("product-" + productId).text("…").metadata(Map.of("productId", productId)).build();
	}

	static ProductResponse product(Long id, String name) {
		return new ProductResponse(id, name, null, new BigDecimal("8000"), 9, 1L, "Mode", List.of(), null, "SHA-" + id,
				null, null);
	}

}
