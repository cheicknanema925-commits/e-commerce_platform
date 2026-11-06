package com.boutique.agent.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.boutique.agent.ProposalStep;
import com.boutique.agent.ToolRunner;
import com.boutique.agent.TurnContext;
import com.boutique.agent.UntrustedText;
import com.boutique.cart.CartResponse;
import com.boutique.cart.CartService;
import com.boutique.catalog.ProductResponse;
import com.boutique.catalog.ProductService;
import com.boutique.order.OrderResponse;
import com.boutique.order.OrderService;
import com.boutique.preorder.PreOrderService;
import com.boutique.review.ReviewService;

// Outils de l'assistant CLIENT. Règles communes :
// - l'identité vient du TurnContext (jeton de l'utilisateur), JAMAIS d'un paramètre rempli par le modèle ;
// - chaque outil appelle un service existant : mêmes règles que l'API (IDOR, stock, prix relus en base) ;
// - les actions ne sont que PROPOSÉES ; elles s'exécutent quand l'utilisateur clique sur « Confirmer ».
@Component
public class ClientTools {

	static final int MAX_RESULTS = 5;

	public static final String PRODUCT_PARAM = "Produit : son nom tel que dit par le client (par exemple « pagne wax ») ou son "
			+ "numéro lu dans un résultat d'outil. N'invente jamais de numéro.";

	private final ProductService productService;

	private final ReviewService reviewService;

	private final OrderService orderService;

	private final CartService cartService;

	private final ToolRunner runner;

	private final ProductFinder productFinder;

	private final PreOrderService preOrderService;

	public ClientTools(ProductService productService, ReviewService reviewService, OrderService orderService,
			CartService cartService, ToolRunner runner, ProductFinder productFinder, PreOrderService preOrderService) {
		this.productService = productService;
		this.reviewService = reviewService;
		this.orderService = orderService;
		this.cartService = cartService;
		this.runner = runner;
		this.productFinder = productFinder;
		this.preOrderService = preOrderService;
	}

	@Tool(description = "Cherche des produits du catalogue Shop Africa par mots-clés (nom, marque ou référence). "
			+ "Renvoie au plus 5 produits avec leur prix et leur stock réels.")
	public Object searchProducts(@ToolParam(description = "Mots-clés, par exemple « sac à dos » ou « boubou »") String query,
			ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "searchProducts", query, () -> {
			List<ProductResponse> found = productFinder.search(query);
			found.forEach(turn::seeProduct);
			return found.stream().map(ClientTools::summary).toList();
		});
	}

	@Tool(description = "Détail d'un produit : description, prix, stock, note moyenne et derniers avis clients.")
	public Object getProductDetails(@ToolParam(description = PRODUCT_PARAM) String product, ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "getProductDetails", product, () -> withProduct(product, turn, (found) -> {
			Long productId = found.id();
			Map<String, Object> details = new LinkedHashMap<>(summary(found));
			if (found.description() != null) {
				details.put("description", found.description());
			}
			var rating = reviewService.rating(productId);
			details.put("noteMoyenne", rating.count() == 0 ? "aucun avis" : rating.average() + " / 5 (" + rating.count() + " avis)");
			// Texte écrit par des clients : présenté comme une DONNÉE à citer, jamais comme une consigne à suivre
			List<String> reviews = reviewService
				.findByProduct(productId, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt")))
				.content()
				.stream()
				.map(UntrustedText::quoteReview)
				.toList();
			details.put(UntrustedText.REVIEWS_KEY, reviews);
			return details;
		}));
	}

	@Tool(description = "Commandes du client connecté : sans numéro, ses 5 dernières commandes ; avec un numéro, "
			+ "le détail et le suivi de livraison de cette commande. Ne donne jamais accès aux commandes d'un autre client.")
	public Object getMyOrderStatus(@ToolParam(description = "Numéro de la commande (facultatif)", required = false) Long orderId,
			ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "getMyOrderStatus", String.valueOf(orderId), () -> {
			if (orderId == null) {
				return orderService.findOrdersOfUser(turn.userId())
					.stream()
					.limit(MAX_RESULTS)
					.map((order) -> orderSummary(turn, order))
					.toList();
			}
			// findOrderOfUser : la commande d'un autre client est « introuvable » (même règle que l'API)
			Map<String, Object> details = orderSummary(turn, orderService.findOrderOfUser(turn.userId(), orderId));
			var delivery = orderService.findDeliveryOfUser(turn.userId(), orderId);
			details.put("livraison", delivery.status() + (delivery.courierName() != null ? ", livreur : " + delivery.courierName() : ""));
			return details;
		});
	}

	@Tool(description = "Contenu actuel du panier du client connecté, avec le total.")
	public Object getMyCart(ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "getMyCart", "", () -> cartView(turn, cartService.getCart(turn.userId())));
	}

	@Tool(description = "PRÉPARE l'ajout d'un produit au panier. Rien n'est ajouté tant que le client n'a pas cliqué "
			+ "sur le bouton « Confirmer » affiché sous ta réponse. Ne dis jamais que c'est déjà fait.")
	public Object proposeAddToCart(@ToolParam(description = PRODUCT_PARAM) String product,
			@ToolParam(description = "Quantité souhaitée (1 à 99)") Integer quantity, ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		String arguments = product + " x " + quantity;
		if (quantity == null || quantity < 1 || quantity > 99) {
			return runner.refuse(turn, "proposeAddToCart", arguments, "La quantité doit être comprise entre 1 et 99.");
		}
		return runner.run(turn, "proposeAddToCart", arguments, () -> withProduct(product, turn, (found) -> {
			if (found.stock() == 0) {
				// Un petit modèle ne fait pas toujours l'étape suivante : l'outil prépare lui-même la pré-commande.
				// Le client voit « Être prévenu du retour de… (rien n'est réservé) » et décide
				preOrderService.checkCanPreOrder(found.id(), quantity);
				turn.propose(new ProposalStep(PreOrderHandler.TYPE, Map.of("productId", found.id(), "quantity", quantity),
						PreOrderHandler.summary(found.name(), quantity)));
				return "Produit en rupture de stock : à la place, une pré-commande est préparée (le client sera prévenu "
						+ "à son retour, rien n'est réservé). Il doit la confirmer avec le bouton « Confirmer ».";
			}
			if (found.stock() < quantity) {
				return Map.of("erreur", "Stock insuffisant : " + found.stock() + " disponible(s).");
			}
			turn.propose(new ProposalStep(AddToCartHandler.TYPE, Map.of("productId", found.id(), "quantity", quantity),
					"Ajouter " + quantity + " × " + found.name() + " au panier (" + found.price().toBigInteger() + " FCFA l'unité)"));
			return "Action préparée : le client doit la confirmer avec le bouton « Confirmer ».";
		}));
	}

	@Tool(description = "PRÉPARE le changement de quantité d'un article DÉJÀ dans le panier (0 = le retirer). "
			+ "Rien ne change tant que le client n'a pas cliqué sur « Confirmer ».")
	public Object proposeCartQuantity(@ToolParam(description = PRODUCT_PARAM) String product,
			@ToolParam(description = "Nouvelle quantité (0 à 99, 0 = retirer)") Integer quantity, ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		String arguments = product + " -> " + quantity;
		if (quantity == null || quantity < 0 || quantity > 99) {
			return runner.refuse(turn, "proposeCartQuantity", arguments, "La quantité doit être comprise entre 0 et 99.");
		}
		return runner.run(turn, "proposeCartQuantity", arguments, () -> withProduct(product, turn, (found) -> {
			var item = cartService.getCart(turn.userId())
				.items()
				.stream()
				.filter((line) -> line.productId().equals(found.id()))
				.findFirst()
				.orElse(null);
			if (item == null) {
				return Map.of("erreur", found.name() + " n'est pas dans le panier : appelle getMyCart pour voir son contenu.");
			}
			if (quantity > found.stock()) {
				return Map.of("erreur", "Stock insuffisant : " + found.stock() + " disponible(s).");
			}
			turn.propose(new ProposalStep(CartQuantityHandler.TYPE, Map.of("productId", found.id(), "quantity", quantity),
					quantity == 0 ? "Retirer " + found.name() + " du panier"
							: "Passer " + found.name() + " de " + item.quantity() + " à " + quantity + " dans le panier"));
			return "Modification préparée : le client doit la confirmer avec le bouton « Confirmer ».";
		}));
	}

	// Exécute l'action sur le produit désigné, ou renvoie au modèle le problème (introuvable, plusieurs candidats)
	public static Object withProduct(String reference, TurnContext turn, ProductFinder finder,
			Function<ProductResponse, Object> action) {
		return switch (finder.resolve(reference, turn)) {
			case ProductFinder.Resolution.Found found -> action.apply(found.product());
			case ProductFinder.Resolution.Problem problem -> problem.answer();
		};
	}

	private Object withProduct(String reference, TurnContext turn, Function<ProductResponse, Object> action) {
		return withProduct(reference, turn, productFinder, action);
	}

	public static Map<String, Object> summary(ProductResponse product) {
		Map<String, Object> view = new LinkedHashMap<>();
		view.put("id", product.id());
		view.put("nom", product.name());
		// Champ vide = absent : un « marque: null » pousse un petit modèle à commenter (ou inventer) la marque
		if (product.brand() != null) {
			view.put("marque", product.brand());
		}
		view.put("categorie", product.categoryName());
		view.put("prixFcfa", product.price().toBigInteger());
		if (product.compareAtPrice() != null) {
			view.put("prixAvantRemiseFcfa", product.compareAtPrice().toBigInteger());
		}
		view.put("stock", product.stock());
		return view;
	}

	private static Map<String, Object> orderSummary(TurnContext turn, OrderResponse order) {
		turn.seeAmount(order.totalAmount());
		Map<String, Object> view = new LinkedHashMap<>();
		view.put("numero", order.id());
		view.put("statut", order.status());
		view.put("paiement", order.paymentMethod());
		view.put("totalFcfa", order.totalAmount().toBigInteger());
		view.put("date", order.createdAt());
		view.put("articles", order.items().stream().map((i) -> i.quantity() + " × " + i.productName()).toList());
		return view;
	}

	private static Map<String, Object> cartView(TurnContext turn, CartResponse cart) {
		turn.seeAmount(cart.total());
		cart.items().forEach((item) -> {
			turn.seeAmount(item.unitPrice());
			turn.seeAmount(item.subtotal());
		});
		Map<String, Object> view = new LinkedHashMap<>();
		view.put("articles", cart.items()
			.stream()
			.map((i) -> Map.of("productId", i.productId(), "nom", i.productName(), "quantite", i.quantity(),
					"sousTotalFcfa", i.subtotal().toBigInteger()))
			.toList());
		view.put("totalFcfa", cart.total().toBigInteger());
		return view;
	}

}
