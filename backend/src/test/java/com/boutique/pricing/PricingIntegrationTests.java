package com.boutique.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.boutique.TestcontainersConfiguration;
import com.boutique.catalog.Category;
import com.boutique.catalog.CategoryRepository;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductRepository;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Les limites des changements de prix sont dans le CODE : chaque test ci-dessous les attaque directement
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PricingIntegrationTests {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Africa/Dakar"));

	@Autowired
	private PricingService pricingService;

	@Autowired
	private PriceChangeRepository changeRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MockMvc mockMvc;

	private Long sandalesId;

	private Long pagneId;

	private Long adminId;

	@BeforeEach
	void setUp() {
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		sandalesId = productRepository.save(new Product(mode, "Sandales cuir", null, new BigDecimal("8000"), 9)).getId();
		pagneId = productRepository.save(new Product(mode, "Pagne wax", null, new BigDecimal("12000"), 5)).getId();
		adminId = userRepository.save(new User("mariama@example.com", "Mariama Ba", Role.ADMIN)).getId();
	}

	@AfterEach
	void cleanUp() {
		changeRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void promotionShowsAStrikethroughPriceAndIsJournaled() {
		PriceChangeRequest request = promotion(20, TODAY.plusDays(7), sandalesId, pagneId);
		PricePlan preview = pricingService.preview(request);

		assertThat(preview.lines()).extracting(PricePlan.Line::newPrice)
			.usingElementComparator(BigDecimal::compareTo)
			.containsExactly(new BigDecimal("6400"), new BigDecimal("9600"));
		// L'aperçu n'écrit rien
		assertThat(price(sandalesId)).isEqualByComparingTo("8000");

		pricingService.apply(adminId, request, preview.expectedOldPrices());

		Product sandales = productRepository.findById(sandalesId).orElseThrow();
		assertThat(sandales.getPrice()).isEqualByComparingTo("6400");
		assertThat(sandales.getCompareAtPrice()).isEqualByComparingTo("8000");
		assertThat(sandales.getDiscountPercent()).isEqualTo(20);
		assertThat(changeRepository.findAll()).hasSize(2)
			.allSatisfy((change) -> assertThat(change.getKind()).isEqualTo(PriceChange.Kind.PROMOTION))
			.extracting(PriceChange::getBatchId)
			.containsOnly(changeRepository.findAll().getFirst().getBatchId());
		assertThat(changeRepository.findAll().getFirst().getOldPrice()).isEqualByComparingTo("8000");
	}

	@Test
	void limitsAreEnforcedByTheCode() {
		refused(promotion(100, TODAY.plusDays(7), sandalesId), "70 %");
		refused(promotion(71, TODAY.plusDays(7), sandalesId), "70 %");
		refused(promotion(0, TODAY.plusDays(7), sandalesId), "70 %");
		refused(promotion(20, null, sandalesId), "date de fin");
		refused(promotion(20, TODAY.minusDays(1), sandalesId), "date de fin");
		refused(promotion(20, TODAY.plusDays(91), sandalesId), "90 jours");
		refused(newPrice("0", sandalesId), "supérieur à 0");
		refused(newPrice("-500", sandalesId), "supérieur à 0");
		refused(newPrice("7999.50", sandalesId), "entier");
		// 8000 → 2000 : baisse de 75 %
		refused(newPrice("2000", sandalesId), "70 %");
		// 8000 → 80000 : faute de frappe probable
		refused(newPrice("80000", sandalesId), "double");
		refused(newPrice("9000", sandalesId, pagneId), "produit par produit");
		refused(new PriceChangeRequest(List.of(), 20, null, TODAY.plusDays(7)), "1 à 50");
		refused(new PriceChangeRequest(List.of(sandalesId), 20, new BigDecimal("9000"), TODAY.plusDays(7)), "soit");

		assertThat(price(sandalesId)).isEqualByComparingTo("8000");
		assertThat(changeRepository.findAll()).isEmpty();
	}

	@Test
	void productAlreadyOnPromotionIsRefused() {
		apply(promotion(10, TODAY.plusDays(3), sandalesId));

		refused(promotion(30, TODAY.plusDays(3), sandalesId), "déjà en promotion");
		refused(newPrice("9000", sandalesId), "déjà en promotion");
	}

	@Test
	void stalePreviewChangesNothing() {
		PriceChangeRequest request = promotion(20, TODAY.plusDays(7), sandalesId, pagneId);
		Map<Long, BigDecimal> expected = pricingService.preview(request).expectedOldPrices();
		// Entre l'aperçu et le clic, l'admin a changé le prix du pagne dans sa fiche
		jdbcTemplate.update("update products set price = 11000 where id = ?", pagneId);

		assertThatThrownBy(() -> pricingService.apply(adminId, request, expected))
			.isInstanceOf(PricesChangedException.class)
			.hasMessageContaining("Pagne wax");
		// Tout ou rien : les sandales n'ont pas bougé non plus
		assertThat(price(sandalesId)).isEqualByComparingTo("8000");
		assertThat(changeRepository.findAll()).isEmpty();
	}

	@Test
	void undoRestoresTheWholeLastBatch() {
		apply(newPrice("9000", pagneId));
		apply(promotion(25, TODAY.plusDays(7), sandalesId));
		assertThat(pricingService.lastBatch()).singleElement()
			.satisfies((change) -> assertThat(change.getProduct().getId()).isEqualTo(sandalesId));

		pricingService.undo(pricingService.lastBatch().getFirst().getBatchId());

		Product sandales = productRepository.findById(sandalesId).orElseThrow();
		assertThat(sandales.getPrice()).isEqualByComparingTo("8000");
		assertThat(sandales.getCompareAtPrice()).isNull();
		// Le changement précédent devient « le dernier » ; il n'a pas été touché
		assertThat(price(pagneId)).isEqualByComparingTo("9000");
		assertThat(pricingService.lastBatch()).singleElement()
			.satisfies((change) -> assertThat(change.getProduct().getId()).isEqualTo(pagneId));
	}

	@Test
	void undoNeverOverwritesALaterManualChange() {
		apply(newPrice("9000", pagneId));
		jdbcTemplate.update("update products set price = 9500 where id = ?", pagneId);

		assertThatThrownBy(() -> pricingService.undo(pricingService.lastBatch().getFirst().getBatchId()))
			.isInstanceOf(PricesChangedException.class);
		assertThat(price(pagneId)).isEqualByComparingTo("9500");
	}

	@Test
	void endedPromotionsGiveBackTheirPrices() {
		apply(promotion(20, TODAY.plusDays(7), sandalesId, pagneId));
		// Pagne repassé à la main entre-temps : on ne l'écrase pas
		jdbcTemplate.update("update products set price = 11000, compare_at_price = null where id = ?", pagneId);
		jdbcTemplate.update("update price_changes set ends_at = now() - interval '1 minute'");

		assertThat(pricingService.expirePromotions()).isEqualTo(2);

		Product sandales = productRepository.findById(sandalesId).orElseThrow();
		assertThat(sandales.getPrice()).isEqualByComparingTo("8000");
		assertThat(sandales.getCompareAtPrice()).isNull();
		assertThat(price(pagneId)).isEqualByComparingTo("11000");
		assertThat(changeRepository.findAll()).extracting(PriceChange::getRevertReason)
			.containsExactlyInAnyOrder(PriceChange.RevertReason.EXPIRED, PriceChange.RevertReason.OVERRIDDEN);
		assertThat(pricingService.expirePromotions()).isZero();
	}

	@Test
	void databaseRefusesAPromotionWithoutEndEvenIfTheCodeWereBypassed() {
		assertThatThrownBy(() -> jdbcTemplate.update("""
				insert into price_changes (batch_id, product_id, kind, old_price, new_price)
				values (gen_random_uuid(), ?, 'PROMOTION', 8000, 2400)""", sandalesId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void journalIsVisibleToAdminsOnly() throws Exception {
		apply(newPrice("9000", pagneId));

		mockMvc.perform(get("/api/admin/price-changes").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(jsonPath("$.content[0].productName").value("Pagne wax"))
			.andExpect(jsonPath("$.content[0].authorName").value("Mariama Ba"))
			.andExpect(jsonPath("$.content[0].oldPrice").value(12000))
			.andExpect(jsonPath("$.content[0].newPrice").value(9000));
		mockMvc.perform(get("/api/admin/price-changes").with(jwt().jwt((t) -> t.subject(String.valueOf(adminId)))))
			.andExpect(status().isForbidden());
	}

	private void apply(PriceChangeRequest request) {
		pricingService.apply(adminId, request, pricingService.preview(request).expectedOldPrices());
	}

	private void refused(PriceChangeRequest request, String reason) {
		assertThatThrownBy(() -> apply(request)).isInstanceOf(InvalidPriceChangeException.class).hasMessageContaining(reason);
	}

	private BigDecimal price(Long productId) {
		return productRepository.findById(productId).orElseThrow().getPrice();
	}

	private static PriceChangeRequest promotion(int percent, LocalDate endDate, Long... productIds) {
		return new PriceChangeRequest(List.of(productIds), percent, null, endDate);
	}

	private static PriceChangeRequest newPrice(String price, Long... productIds) {
		return new PriceChangeRequest(List.of(productIds), null, new BigDecimal(price), null);
	}

}
