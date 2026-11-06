package com.boutique.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.catalog.Product;
import com.boutique.catalog.ProductNotFoundException;
import com.boutique.catalog.ProductRepository;
import com.boutique.common.PageResponse;
import com.boutique.user.UserRepository;

// Changements de prix en masse (assistant admin). Les LIMITES sont ici, dans le code : aucun texte (prompt, avis
// piégé, modèle qui se trompe) ne peut les dépasser. Une seule méthode calcule le plan : aperçu et application
// passent par elle, on ne peut donc pas appliquer un changement qui n'aurait pas passé les contrôles.
@Service
@Transactional(readOnly = true)
public class PricingService {

	private static final Logger log = LoggerFactory.getLogger(PricingService.class);

	public static final int MAX_DISCOUNT_PERCENT = 70;

	public static final int MAX_PRODUCTS = 50;

	public static final int MAX_PROMOTION_DAYS = 90;

	private static final ZoneId SHOP_ZONE = ZoneId.of("Africa/Dakar");

	private final PriceChangeRepository changeRepository;

	private final ProductRepository productRepository;

	private final UserRepository userRepository;

	private final Clock clock;

	public PricingService(PriceChangeRepository changeRepository, ProductRepository productRepository,
			UserRepository userRepository) {
		this.changeRepository = changeRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
		this.clock = Clock.systemUTC();
	}

	public PricePlan preview(PriceChangeRequest request) {
		return plan(request, loadProducts(request));
	}

	// Tout ou rien, dans UNE transaction : si un prix a bougé depuis l'aperçu, rien n'est modifié
	@Transactional
	public PricePlan apply(Long authorId, PriceChangeRequest request, Map<Long, BigDecimal> expectedOldPrices) {
		List<Product> products = loadProducts(request);
		PricePlan plan = plan(request, products);
		for (PricePlan.Line line : plan.lines()) {
			BigDecimal expected = expectedOldPrices.get(line.productId());
			if (expected == null || expected.compareTo(line.oldPrice()) != 0) {
				throw new PricesChangedException(line.productName());
			}
		}
		UUID batchId = UUID.randomUUID();
		var author = userRepository.getReferenceById(authorId);
		for (int i = 0; i < products.size(); i++) {
			Product product = products.get(i);
			PricePlan.Line line = plan.lines().get(i);
			// Journal AVANT la modification : il lit l'ancien prix sur le produit
			changeRepository.save(new PriceChange(batchId, product, author, plan.kind(), line.newPrice(),
					line.newCompareAtPrice(), plan.endsAt()));
			product.reprice(line.newPrice(), line.newCompareAtPrice());
		}
		log.info("Changement de prix {} ({} produit(s), {}) par l'utilisateur {}", batchId, products.size(), plan.kind(),
				authorId);
		return plan;
	}

	// Le dernier lot encore en place : ce que l'annulation rétablirait
	public List<PriceChange> lastBatch() {
		return changeRepository.findFirstByRevertedAtIsNullOrderByIdDesc()
			.map((change) -> changeRepository.findByBatchIdAndRevertedAtIsNullOrderById(change.getBatchId()))
			.orElse(List.of());
	}

	// Annule un lot entier. Si quelqu'un a modifié un des prix depuis, on refuse : on n'écrase pas son travail
	@Transactional
	public int undo(UUID batchId) {
		List<PriceChange> changes = changeRepository.findByBatchIdAndRevertedAtIsNullOrderById(batchId);
		if (changes.isEmpty()) {
			throw new NothingToUndoException();
		}
		for (PriceChange change : changes) {
			if (!change.isStillInPlace()) {
				throw new PricesChangedException(change.getProduct().getName());
			}
		}
		Instant now = clock.instant();
		changes.forEach((change) -> change.revert(PriceChange.RevertReason.UNDO, now));
		return changes.size();
	}

	// Fin des promotions (PromotionExpiryJob) : prix d'avant rétablis, sauf si le prix a été changé entre-temps
	@Transactional
	public int expirePromotions() {
		Instant now = clock.instant();
		List<PriceChange> due = changeRepository.findDue(PriceChange.Kind.PROMOTION, now);
		for (PriceChange change : due) {
			change.revert(change.isStillInPlace() ? PriceChange.RevertReason.EXPIRED : PriceChange.RevertReason.OVERRIDDEN,
					now);
		}
		return due.size();
	}

	public PageResponse<PriceChangeResponse> history(Pageable pageable) {
		return PageResponse.from(changeRepository.findAllBy(pageable).map(PriceChangeResponse::from));
	}

	private PricePlan plan(PriceChangeRequest request, List<Product> products) {
		boolean promotion = request.percent() != null;
		if (promotion == (request.newPrice() != null)) {
			throw new InvalidPriceChangeException("Indiquez soit une remise en pourcentage, soit un nouveau prix.");
		}
		Instant endsAt = promotion ? promotionEnd(request.endDate()) : null;
		if (!promotion && request.endDate() != null) {
			throw new InvalidPriceChangeException("Un nouveau prix est permanent : pour une durée limitée, faites une promotion.");
		}
		List<PricePlan.Line> lines = new ArrayList<>();
		for (Product product : products) {
			if (product.getCompareAtPrice() != null) {
				throw new InvalidPriceChangeException("« " + product.getName()
						+ " » est déjà en promotion : annulez-la avant de changer son prix.");
			}
			lines.add(promotion ? promotionLine(product, request.percent()) : priceLine(product, request.newPrice()));
		}
		return new PricePlan(promotion ? PriceChange.Kind.PROMOTION : PriceChange.Kind.PRICE, List.copyOf(lines), endsAt);
	}

	private static PricePlan.Line promotionLine(Product product, int percent) {
		if (percent < 1 || percent > MAX_DISCOUNT_PERCENT) {
			throw new InvalidPriceChangeException(
					"Remise refusée : elle doit être comprise entre 1 et " + MAX_DISCOUNT_PERCENT + " %.");
		}
		BigDecimal regular = product.getPrice();
		// Arrondi au franc INFÉRIEUR : le client paie au plus le prix annoncé
		BigDecimal discounted = regular.multiply(BigDecimal.valueOf(100 - percent))
			.divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
		int realDiscount = discountPercent(regular, discounted);
		if (discounted.signum() <= 0 || realDiscount > MAX_DISCOUNT_PERCENT) {
			throw new InvalidPriceChangeException("Remise refusée pour « " + product.getName() + " » : prix trop bas.");
		}
		return new PricePlan.Line(product.getId(), product.getName(), regular, discounted, regular, realDiscount);
	}

	private static PricePlan.Line priceLine(Product product, BigDecimal newPrice) {
		BigDecimal current = product.getPrice();
		if (newPrice.signum() <= 0 || newPrice.stripTrailingZeros().scale() > 0) {
			throw new InvalidPriceChangeException("Le prix doit être un nombre entier de FCFA supérieur à 0.");
		}
		// Même plafond qu'une promotion pour une baisse ; une hausse au-delà du double ressemble à une faute de frappe
		if (discountPercent(current, newPrice) > MAX_DISCOUNT_PERCENT) {
			throw new InvalidPriceChangeException("Baisse refusée : " + MAX_DISCOUNT_PERCENT + " % maximum par rapport au prix actuel.");
		}
		if (newPrice.compareTo(current.multiply(BigDecimal.TWO)) > 0) {
			throw new InvalidPriceChangeException("Hausse refusée : le nouveau prix dépasse le double du prix actuel.");
		}
		return new PricePlan.Line(product.getId(), product.getName(), current, newPrice.setScale(0, RoundingMode.UNNECESSARY),
				null, null);
	}

	private Instant promotionEnd(LocalDate endDate) {
		if (endDate == null) {
			throw new InvalidPriceChangeException("Une promotion doit avoir une date de fin.");
		}
		LocalDate today = LocalDate.now(clock.withZone(SHOP_ZONE));
		if (endDate.isBefore(today) || endDate.isAfter(today.plusDays(MAX_PROMOTION_DAYS))) {
			throw new InvalidPriceChangeException("La date de fin doit être comprise entre aujourd'hui et dans "
					+ MAX_PROMOTION_DAYS + " jours.");
		}
		// La promotion dure jusqu'à la fin de cette journée, heure de Dakar
		return endDate.plusDays(1).atStartOfDay(SHOP_ZONE).toInstant();
	}

	private List<Product> loadProducts(PriceChangeRequest request) {
		List<Long> ids = (request.productIds() == null) ? List.of() : List.copyOf(new LinkedHashSet<>(request.productIds()));
		if (ids.isEmpty() || ids.size() > MAX_PRODUCTS) {
			throw new InvalidPriceChangeException("Un changement de prix porte sur 1 à " + MAX_PRODUCTS + " produits.");
		}
		if (request.newPrice() != null && ids.size() > 1) {
			throw new InvalidPriceChangeException("Un nouveau prix se fixe produit par produit.");
		}
		List<Product> products = new ArrayList<>();
		for (Long id : ids) {
			products.add(productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id)));
		}
		return products;
	}

	private static int discountPercent(BigDecimal before, BigDecimal after) {
		return before.subtract(after).multiply(BigDecimal.valueOf(100)).divide(before, 0, RoundingMode.FLOOR).intValue();
	}

}
