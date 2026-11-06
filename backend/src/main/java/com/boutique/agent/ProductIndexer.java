package com.boutique.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.boutique.catalog.ProductChangedEvent;
import com.boutique.catalog.ProductResponse;
import com.boutique.catalog.ProductService;
import com.boutique.common.PageResponse;
import com.boutique.common.NotFoundException;

// Recherche par le SENS (RAG) : chaque produit est transformé en vecteur (embedding) et rangé dans pgvector.
// Une question proche par le sens donne un vecteur proche, même sans mot en commun (« sandalles » → « Sandales »).
// L'index ne contient que du texte descriptif : prix et stock sont toujours relus en base au moment de répondre.
@Component
public class ProductIndexer {

	private static final Logger log = LoggerFactory.getLogger(ProductIndexer.class);

	private static final double MIN_SIMILARITY = 0.55;

	private final VectorStore vectorStore;

	private final ProductService productService;

	// false (tests) : index jamais lu ni écrit, la recherche se limite aux mots-clés (résultats déterministes)
	private final boolean enabled;

	public ProductIndexer(VectorStore vectorStore, ProductService productService,
			@Value("${boutique.agent.semantic-search:true}") boolean enabled) {
		this.vectorStore = vectorStore;
		this.productService = productService;
		this.enabled = enabled;
	}

	// Au démarrage, en arrière-plan : (ré)indexe tout le catalogue (upsert : pas de doublon)
	@Async
	@EventListener(ApplicationReadyEvent.class)
	public void indexAllOnStartup() {
		if (enabled) {
			reindexAll();
		}
	}

	public int reindexAll() {
		int count = 0;
		try {
			for (int page = 0;; page++) {
				PageResponse<ProductResponse> products = productService.findAll(null, null, false,
						PageRequest.of(page, 100, Sort.by("id")));
				vectorStore.add(products.content().stream().map(ProductIndexer::document).toList());
				count += products.content().size();
				if (page + 1 >= products.totalPages()) {
					break;
				}
			}
			log.info("Index sémantique : {} produit(s) indexé(s)", count);
		}
		catch (RuntimeException ex) {
			// Modèle d'embeddings indisponible (Ollama arrêté…) : la boutique fonctionne, seule la recherche par le sens manque
			log.warn("Index sémantique non construit : {}", ex.getMessage());
		}
		return count;
	}

	// Après le commit seulement (jamais d'appel réseau dans une transaction), en arrière-plan
	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onProductChanged(ProductChangedEvent event) {
		if (!enabled) {
			return;
		}
		try {
			if (event.deleted()) {
				vectorStore.delete(List.of(documentId(event.productId())));
			}
			else {
				vectorStore.add(List.of(document(productService.findById(event.productId()))));
			}
		}
		catch (RuntimeException ex) {
			log.warn("Produit {} non réindexé : {}", event.productId(), ex.getMessage());
		}
	}

	// Identifiants des produits les plus proches par le sens (vide si l'index est indisponible)
	public List<Long> similarProductIds(String query, int max) {
		if (!enabled) {
			return List.of();
		}
		try {
			List<Long> ids = new ArrayList<>();
			for (Document document : vectorStore.similaritySearch(SearchRequest.builder()
				.query(query)
				.topK(max)
				.similarityThreshold(MIN_SIMILARITY)
				.build())) {
				if (document.getMetadata().get("productId") instanceof Number id) {
					ids.add(id.longValue());
				}
			}
			return ids;
		}
		catch (RuntimeException ex) {
			log.warn("Recherche sémantique indisponible : {}", ex.getMessage());
			return List.of();
		}
	}

	// Produits correspondants, relus EN BASE (un produit supprimé entre-temps est ignoré)
	public List<ProductResponse> similarProducts(String query, int max) {
		List<ProductResponse> products = new ArrayList<>();
		for (Long id : similarProductIds(query, max)) {
			try {
				products.add(productService.findById(id));
			}
			catch (NotFoundException ex) {
				// produit supprimé, index pas encore à jour
			}
		}
		return products;
	}

	static String documentId(Long productId) {
		return "product-" + productId;
	}

	private static Document document(ProductResponse product) {
		String text = "Produit : " + product.name() + ". Catégorie : " + product.categoryName() + "."
				+ (product.brand() != null ? " Marque : " + product.brand() + "." : "")
				+ (product.description() != null ? " " + product.description() : "");
		return Document.builder()
			.id(documentId(product.id()))
			.text(text)
			.metadata(Map.of("productId", product.id(), "category", product.categoryName()))
			.build();
	}

}
