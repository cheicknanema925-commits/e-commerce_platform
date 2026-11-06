package com.boutique.agent.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.boutique.agent.ProductIndexer;
import com.boutique.agent.TurnContext;
import com.boutique.catalog.ProductResponse;
import com.boutique.catalog.ProductService;
import com.boutique.common.NotFoundException;

// Trouver un produit pour un PETIT modèle : il ne sait pas toujours enchaîner « chercher → noter l'id → agir » et
// invente parfois un numéro. Les outils d'action acceptent donc le NOM dit par le client ; ce composant fait la
// recherche, et en cas de doute renvoie les candidats pour que le modèle demande au client lequel il veut.
@Component
public class ProductFinder {

	static final int MAX_RESULTS = 5;

	private final ProductService productService;

	private final ProductIndexer productIndexer;

	public ProductFinder(ProductService productService, ProductIndexer productIndexer) {
		this.productService = productService;
		this.productIndexer = productIndexer;
	}

	// Mots-clés d'abord (exacts), complétés par la recherche par le sens (fautes de frappe, synonymes, « un cadeau
	// pour ma mère »). Les produits trouvés par le sens sont relus en base : prix et stock exacts
	public List<ProductResponse> search(String query) {
		List<ProductResponse> found = new ArrayList<>(keywordSearch(query));
		if (found.size() < MAX_RESULTS && query != null && !query.isBlank()) {
			productIndexer.similarProducts(query, MAX_RESULTS)
				.stream()
				.filter((p) -> found.stream().noneMatch((f) -> f.id().equals(p.id())))
				.limit(MAX_RESULTS - found.size())
				.forEach(found::add);
		}
		return found;
	}

	// Produit désigné par le modèle : numéro, ou nom. Seule une correspondance par mots-clés sans ambiguïté est
	// choisie d'office ; sinon les candidats sont renvoyés (le client tranchera, puis confirmera de toute façon)
	public Resolution resolve(String reference, TurnContext turn) {
		String text = (reference == null) ? "" : reference.strip();
		if (text.isEmpty()) {
			return problem("Quel produit ? Demande-le au client.", List.of(), turn);
		}
		if (text.matches("\\d{1,18}")) {
			try {
				return found(productService.findById(Long.valueOf(text)), turn);
			}
			catch (NotFoundException ex) {
				return problem("Aucun produit n° " + text + ". N'invente pas de numéro : utilise le nom dit par le client.",
						List.of(), turn);
			}
		}
		List<ProductResponse> byKeyword = keywordSearch(text);
		List<ProductResponse> sameName = byKeyword.stream().filter((p) -> p.name().equalsIgnoreCase(text)).toList();
		if (sameName.size() == 1) {
			return found(sameName.getFirst(), turn);
		}
		if (sameName.isEmpty() && byKeyword.size() == 1) {
			return found(byKeyword.getFirst(), turn);
		}
		List<ProductResponse> candidates = byKeyword.isEmpty() ? search(text) : byKeyword;
		if (candidates.isEmpty()) {
			return problem("Aucun produit ne correspond à « " + text + " ».", candidates, turn);
		}
		return problem("Plusieurs produits possibles pour « " + text + " » : demande au client lequel il veut.",
				candidates, turn);
	}

	// Recherche par mots-clés tolérante : la phrase entière, puis chaque mot significatif
	// (un petit modèle écrit parfois « sacsà dos » : on cherche alors « sacsà » et « dos »)
	List<ProductResponse> keywordSearch(String query) {
		String text = (query == null) ? "" : query.strip();
		List<ProductResponse> found = new ArrayList<>(productService
			.findAll(null, text, false, PageRequest.of(0, MAX_RESULTS))
			.content());
		if (found.isEmpty()) {
			for (String word : text.split("[\\s,;.'’-]+")) {
				if (word.length() >= 3 && found.size() < MAX_RESULTS) {
					productService.findAll(null, word, false, PageRequest.of(0, MAX_RESULTS))
						.content()
						.stream()
						.filter((p) -> found.stream().noneMatch((f) -> f.id().equals(p.id())))
						.forEach(found::add);
				}
			}
		}
		return found.stream().limit(MAX_RESULTS).toList();
	}

	private static Resolution found(ProductResponse product, TurnContext turn) {
		turn.seeProduct(product);
		return new Resolution.Found(product);
	}

	// Les candidats deviennent aussi des fiches affichées au client : il choisit sur des données de la base
	private static Resolution problem(String message, List<ProductResponse> candidates, TurnContext turn) {
		candidates.forEach(turn::seeProduct);
		return new Resolution.Problem(candidates.isEmpty() ? Map.of("erreur", message)
				: Map.of("erreur", message, "choix", candidates.stream().map(ClientTools::summary).toList()));
	}

	public sealed interface Resolution {

		record Found(ProductResponse product) implements Resolution {
		}

		record Problem(Map<String, Object> answer) implements Resolution {
		}

	}

}
