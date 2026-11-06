package com.boutique.agent.admin;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.boutique.admin.AdminStatsResponse;
import com.boutique.admin.AdminStatsService;
import com.boutique.admin.SalesPeriod;
import com.boutique.agent.ProposalStep;
import com.boutique.agent.ToolRunner;
import com.boutique.agent.TurnContext;
import com.boutique.agent.UntrustedText;
import com.boutique.agent.client.ClientTools;
import com.boutique.agent.client.ProductFinder;
import com.boutique.catalog.Category;
import com.boutique.catalog.CategoryRepository;
import com.boutique.catalog.ProductResponse;
import com.boutique.catalog.ProductService;
import com.boutique.pricing.PriceChange;
import com.boutique.pricing.PriceChangeRequest;
import com.boutique.pricing.PricePlan;
import com.boutique.pricing.PricingService;
import com.boutique.review.ReviewService;

// Outils de l'assistant ADMIN (route /api/admin/agent : rôle ADMIN exigé par SecurityConfig).
// Les chiffres viennent des services (calculés en base) ; les changements de prix passent par PricingService, qui
// impose les limites, et ne sont que PROPOSÉS : ils s'appliquent au clic de l'admin.
@Component
public class AdminTools {

	private static final ZoneId SHOP_ZONE = ZoneId.of("Africa/Dakar");

	private static final String TARGET = "Produit (nom ou numéro) ou catégorie (par exemple « Mode ») concerné";

	private static final String PERCENT = "Remise en pourcentage pour une promotion, par exemple 20";

	private static final String NEW_PRICE = "Nouveau prix permanent en FCFA, pour un seul produit";

	private static final String END_DATE = "Dernier jour de la promotion, au format AAAA-MM-JJ (obligatoire pour une promotion)";

	private final AdminStatsService statsService;

	private final ProductService productService;

	private final CategoryRepository categoryRepository;

	private final ReviewService reviewService;

	private final PricingService pricingService;

	private final ProductFinder productFinder;

	private final ToolRunner runner;

	public AdminTools(AdminStatsService statsService, ProductService productService,
			CategoryRepository categoryRepository, ReviewService reviewService, PricingService pricingService,
			ProductFinder productFinder, ToolRunner runner) {
		this.statsService = statsService;
		this.productService = productService;
		this.categoryRepository = categoryRepository;
		this.reviewService = reviewService;
		this.pricingService = pricingService;
		this.productFinder = productFinder;
		this.runner = runner;
	}

	@Tool(description = "Statistiques de la boutique calculées en base : chiffre d'affaires encaissé, commandes, panier "
			+ "moyen, ventes par catégorie, meilleures ventes, stocks faibles, et l'évolution par jour ou par mois.")
	public Object getSalesStats(
			@ToolParam(description = "Découpage de l'évolution : DAY (par jour) ou MONTH (par mois)", required = false) String period,
			ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "getSalesStats", String.valueOf(period), () -> {
			AdminStatsResponse stats = statsService.stats();
			SalesPeriod salesPeriod = "MONTH".equalsIgnoreCase(String.valueOf(period)) ? SalesPeriod.MONTH : SalesPeriod.DAY;
			Map<String, Object> view = new LinkedHashMap<>();
			view.put("encaisseTotalFcfa", amount(turn, stats.collectedRevenue()));
			view.put("commandes", stats.orderCount());
			view.put("clients", stats.customerCount());
			view.put("aujourdhui", Map.of("encaisseFcfa", amount(turn, stats.today().collected()), "commandes", stats.today().orders()));
			view.put("ceMois", Map.of("encaisseFcfa", amount(turn, stats.thisMonth().collected()), "commandes", stats.thisMonth().orders()));
			view.put("panierMoyenFcfa", amount(turn, stats.averageOrderValue()));
			view.put("commandesAConfirmer", stats.awaitingConfirmation());
			view.put("especesARecupererFcfa", amount(turn, stats.payments().cashToCollect()));
			view.put("ventesParCategorie", stats.salesByCategory()
				.stream()
				.map((c) -> Map.of("categorie", c.category(), "articles", c.quantity(), "ventesFcfa", amount(turn, c.revenue())))
				.toList());
			view.put("meilleuresVentes", stats.bestSellers().stream().map((b) -> b.name() + " (" + b.quantity() + " vendus)").toList());
			view.put("stocksFaibles", stats.lowStock().stream().map((l) -> l.name() + " (" + l.stock() + " en stock)").toList());
			// Les 12 dernières périodes seulement : assez pour une tendance, sans noyer un petit modèle
			var points = statsService.sales(salesPeriod).points();
			view.put(salesPeriod == SalesPeriod.MONTH ? "evolutionParMois" : "evolutionParJour", points
				.subList(Math.max(0, points.size() - 12), points.size())
				.stream()
				.map((p) -> p.start() + " : " + amount(turn, p.collected()) + " FCFA, " + p.orders() + " commande(s)")
				.toList());
			return view;
		});
	}

	@Tool(description = "Cherche des produits (nom, marque ou référence) : prix, prix barré, stock et référence.")
	public Object searchProducts(@ToolParam(description = "Mots-clés") String query, ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "searchProducts", query, () -> {
			List<ProductResponse> found = productFinder.search(query);
			found.forEach(turn::seeProduct);
			return found.stream().map(AdminTools::adminSummary).toList();
		});
	}

	@Tool(description = "Détail d'un produit : prix, stock, note moyenne et derniers avis clients.")
	public Object getProductDetails(@ToolParam(description = ClientTools.PRODUCT_PARAM) String product,
			ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "getProductDetails", product, () -> ClientTools.withProduct(product, turn, productFinder, (found) -> {
			Long productId = found.id();
			Map<String, Object> details = new LinkedHashMap<>(adminSummary(found));
			var rating = reviewService.rating(productId);
			details.put("noteMoyenne", rating.count() == 0 ? "aucun avis" : rating.average() + " / 5 (" + rating.count() + " avis)");
			details.put(UntrustedText.REVIEWS_KEY, reviewService
				.findByProduct(productId, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt")))
				.content()
				.stream()
				.map(UntrustedText::quoteReview)
				.toList());
			return details;
		}));
	}

	@Tool(description = "APERÇU d'un changement de prix, sans rien modifier : produit par produit, ancien prix, nouveau "
			+ "prix et remise. Soit une promotion (percent + endDate obligatoire), soit un nouveau prix pour UN produit "
			+ "(newPriceFcfa). Montre toujours cet aperçu à l'administrateur avant de préparer le changement.")
	public Object previewPriceChange(@ToolParam(description = TARGET) String target,
			@ToolParam(description = PERCENT, required = false) Integer percent,
			@ToolParam(description = NEW_PRICE, required = false) Long newPriceFcfa,
			@ToolParam(description = END_DATE, required = false) String endDate, ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "previewPriceChange", describe(target, percent, newPriceFcfa, endDate),
				() -> withRequest(target, percent, newPriceFcfa, endDate, turn, (request) -> {
					Map<String, Object> view = view(turn, pricingService.preview(request));
					// Un petit modèle peut s'arrêter là et annoncer « c'est fait » : l'admin sera prévenu du contraire
					turn.remindIfNothingProposed("Aucun prix n'a été modifié : ce n'était qu'un aperçu. Demandez à "
							+ "l'assistant de préparer le changement pour obtenir le bouton « Confirmer ».");
					return view;
				}));
	}

	@Tool(description = "PRÉPARE un changement de prix (mêmes paramètres que previewPriceChange). Rien ne change tant que "
			+ "l'administrateur n'a pas cliqué sur « Confirmer ». Les limites de la boutique (remise de 70 % maximum, date "
			+ "de fin obligatoire pour une promotion) sont appliquées par le système : ne cherche pas à les contourner.")
	public Object applyPriceChange(@ToolParam(description = TARGET) String target,
			@ToolParam(description = PERCENT, required = false) Integer percent,
			@ToolParam(description = NEW_PRICE, required = false) Long newPriceFcfa,
			@ToolParam(description = END_DATE, required = false) String endDate, ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "applyPriceChange", describe(target, percent, newPriceFcfa, endDate),
				() -> withRequest(target, percent, newPriceFcfa, endDate, turn, (request) -> {
					// Aperçu calculé MAINTENANT : la carte montre exactement ce qui sera appliqué, et les prix lus ici
					// servent de contrôle au clic (refus si l'un d'eux a changé entre-temps)
					PricePlan plan = pricingService.preview(request);
					Map<String, Object> params = new HashMap<>();
					params.put("productIds", request.productIds());
					params.put("percent", request.percent());
					params.put("newPrice", request.newPrice() == null ? null : request.newPrice().toPlainString());
					params.put("endDate", request.endDate() == null ? null : request.endDate().toString());
					params.put("expectedOldPrices", plan.expectedOldPrices()
						.entrySet()
						.stream()
						.collect(Collectors.toMap((e) -> String.valueOf(e.getKey()), (e) -> e.getValue().toPlainString())));
					turn.propose(new ProposalStep(ApplyPriceChangeHandler.TYPE, params, summary(plan)));
					Map<String, Object> answer = new LinkedHashMap<>(view(turn, plan));
					answer.put("etat", "Changement préparé : l'administrateur doit le vérifier puis cliquer sur « Confirmer ».");
					return answer;
				}));
	}

	@Tool(description = "PRÉPARE l'annulation du dernier changement de prix (tout le lot) : les anciens prix seront "
			+ "rétablis au clic de l'administrateur sur « Confirmer ».")
	public Object undoLastChange(ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "undoLastChange", "", () -> {
			List<PriceChange> batch = pricingService.lastBatch();
			if (batch.isEmpty()) {
				return Map.of("erreur", "Aucun changement de prix à annuler.");
			}
			String lines = batch.stream().map((change) -> {
				turn.seeAmount(change.getNewPrice());
				turn.seeAmount(change.getOldPrice());
				return change.getProduct().getName() + " " + fcfa(change.getNewPrice()) + " → " + fcfa(change.getOldPrice());
			}).collect(Collectors.joining(" ; "));
			turn.propose(new ProposalStep(UndoPriceChangeHandler.TYPE, Map.of("batchId", batch.getFirst().getBatchId().toString()),
					"Annuler le dernier changement de prix : " + lines));
			return Map.of("etat", "Annulation préparée : l'administrateur doit cliquer sur « Confirmer ».", "retour", lines);
		});
	}

	// Cible → produits, puis demande de changement. Les erreurs de lecture deviennent des réponses au modèle
	private Object withRequest(String target, Integer percent, Long newPriceFcfa, String endDate, TurnContext turn,
			Function<PriceChangeRequest, Object> action) {
		LocalDate end;
		try {
			end = (endDate == null || endDate.isBlank()) ? null : LocalDate.parse(endDate.strip());
		}
		catch (DateTimeParseException ex) {
			return Map.of("erreur", "Date de fin illisible : utilise le format AAAA-MM-JJ (aujourd'hui : "
					+ LocalDate.now(SHOP_ZONE) + ").");
		}
		Object products = targetProducts(target, turn);
		if (!(products instanceof List<?> ids)) {
			return products;
		}
		// Un petit modèle remplit souvent TOUS les paramètres facultatifs avec 0 : 0 veut dire « absent ».
		// Une remise s'exprime en positif : « -20 % » et « 20 % » veulent dire la même chose
		Integer discount = (percent == null || percent == 0) ? null : Math.abs(percent);
		BigDecimal newPrice = (newPriceFcfa == null || newPriceFcfa == 0) ? null : BigDecimal.valueOf(newPriceFcfa);
		return action.apply(new PriceChangeRequest(ids.stream().map((id) -> (Long) id).toList(), discount, newPrice, end));
	}

	// Une catégorie entière, ou un produit. Jamais « tout le catalogue » : la cible doit être nommée
	private Object targetProducts(String target, TurnContext turn) {
		String wanted = normalize(target);
		for (Category category : categoryRepository.findAllByOrderByNameAsc()) {
			if (normalize(category.getName()).equals(wanted) || normalize(category.getSlug()).equals(wanted)) {
				List<Long> ids = productService
					.findAll(category.getSlug(), null, false, PageRequest.of(0, PricingService.MAX_PRODUCTS + 1, Sort.by("id")))
					.content()
					.stream()
					.map(ProductResponse::id)
					.toList();
				return ids.isEmpty() ? Map.of("erreur", "La catégorie " + category.getName() + " ne contient aucun produit.") : ids;
			}
		}
		return switch (productFinder.resolve(target, turn)) {
			case ProductFinder.Resolution.Found found -> List.of(found.product().id());
			case ProductFinder.Resolution.Problem problem -> problem.answer();
		};
	}

	private static Map<String, Object> view(TurnContext turn, PricePlan plan) {
		Map<String, Object> view = new LinkedHashMap<>();
		view.put("type", plan.kind() == PriceChange.Kind.PROMOTION ? "promotion" : "nouveau prix permanent");
		if (plan.endsAt() != null) {
			view.put("dernierJour", plan.endsAt().atZone(SHOP_ZONE).toLocalDate().minusDays(1).toString());
		}
		view.put("produits", plan.lines().stream().map((line) -> {
			turn.seeAmount(line.oldPrice());
			turn.seeAmount(line.newPrice());
			return line.productName() + " : " + fcfa(line.oldPrice()) + " → " + fcfa(line.newPrice())
					+ (line.discountPercent() != null ? " (−" + line.discountPercent() + " %)" : "");
		}).toList());
		return view;
	}

	private static String summary(PricePlan plan) {
		String lines = plan.lines()
			.stream()
			.map((line) -> line.productName() + " " + fcfa(line.oldPrice()) + " → " + fcfa(line.newPrice()))
			.collect(Collectors.joining(" ; "));
		if (plan.kind() == PriceChange.Kind.PRICE) {
			return "Nouveau prix : " + lines;
		}
		// Le nom du jour sur la carte : l'admin repère d'un coup d'œil un « dimanche » mal choisi par le modèle
		LocalDate lastDay = plan.endsAt().atZone(SHOP_ZONE).toLocalDate().minusDays(1);
		return "Promotion −" + plan.lines().getFirst().discountPercent() + " % jusqu'au "
				+ lastDay.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)) + " inclus : " + lines;
	}

	private static Map<String, Object> adminSummary(ProductResponse product) {
		Map<String, Object> view = new LinkedHashMap<>(ClientTools.summary(product));
		view.put("reference", product.sku());
		return view;
	}

	private static Object amount(TurnContext turn, BigDecimal value) {
		turn.seeAmount(value);
		return value == null ? 0 : value.toBigInteger();
	}

	private static String fcfa(BigDecimal value) {
		return value.toBigInteger() + " FCFA";
	}

	private static String describe(String target, Integer percent, Long newPrice, String endDate) {
		return target + " | " + (percent != null ? percent + " %" : newPrice + " FCFA") + " | fin " + endDate;
	}

	private static String normalize(String text) {
		return (text == null) ? "" : Normalizer.normalize(text.strip(), Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT);
	}

}
