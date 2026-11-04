package com.boutique.store;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.boutique.TestcontainersConfiguration;
import com.boutique.cart.CartRepository;
import com.boutique.catalog.Category;
import com.boutique.catalog.CategoryRepository;
import com.boutique.catalog.Product;
import com.boutique.catalog.ProductRepository;
import com.boutique.order.DeliveryRepository;
import com.boutique.order.OrderRepository;
import com.boutique.user.Role;
import com.boutique.user.User;
import com.boutique.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Les paramètres agissent pour de vrai sur la commande : vérifiés par le serveur, pas seulement masqués à l'écran
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class StoreSettingsIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StoreSettingsService storeSettingsService;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private DeliveryRepository deliveryRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserRepository userRepository;

	private RequestPostProcessor awa;

	// Panier d'Awa : 1 boubou à 25 000 FCFA
	@BeforeEach
	void setUp() throws Exception {
		Category mode = categoryRepository.findBySlug("mode").orElseThrow();
		Long productId = productRepository.save(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 5)).getId();
		Long awaId = userRepository.save(new User("awa@example.com", "Awa Diop", Role.CUSTOMER)).getId();
		awa = jwt().jwt((token) -> token.issuer("boutique-api").subject(String.valueOf(awaId)));
		mockMvc.perform(post("/api/cart/items").with(awa)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"productId\": %d, \"quantity\": 1}".formatted(productId))).andExpect(status().isOk());
	}

	// La ligne de paramètres est partagée par tous les tests : on remet toujours les valeurs par défaut
	@AfterEach
	void cleanUp() {
		storeSettingsService.update(new StoreSettingsRequest("Shop Africa", "contact@shopafrica.sn", true, true, null,
				BigDecimal.ZERO, List.of()));
		cartRepository.deleteAll();
		deliveryRepository.deleteAll();
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void anyoneCanReadTheSettingsButOnlyAnAdminChangesThem() throws Exception {
		mockMvc.perform(get("/api/store/settings"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.storeName").value("Shop Africa"))
			.andExpect(jsonPath("$.codEnabled").value(true));
		mockMvc.perform(put("/api/admin/settings").with(awa).contentType(MediaType.APPLICATION_JSON).content(settingsJson(true, "null", "0", "[]")))
			.andExpect(status().isForbidden());
	}

	@Test
	void cashOrderPaysTheServiceFee() throws Exception {
		updateSettings(true, "null", "500", "[]").andExpect(status().isOk());

		checkout("CASH_ON_DELIVERY", "Dakar")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.serviceFee").value(500))
			.andExpect(jsonPath("$.totalAmount").value(25500));
	}

	@Test
	void disabledCashOrUnservedCityIsRefused() throws Exception {
		updateSettings(true, "null", "0", "[\"Dakar\", \"Pikine\"]");
		checkout("CASH_ON_DELIVERY", "Thiès")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Le paiement à la livraison n'est pas proposé à Thiès. Villes desservies : Dakar, Pikine."));

		updateSettings(false, "null", "0", "[]");
		checkout("CASH_ON_DELIVERY", "Dakar").andExpect(status().isConflict());
	}

	@Test
	void cashAboveTheCeilingIsRefused() throws Exception {
		updateSettings(true, "20000", "0", "[]");

		checkout("CASH_ON_DELIVERY", "Dakar").andExpect(status().isConflict());
	}

	private ResultActions updateSettings(boolean codEnabled, String max, String fee, String zones) throws Exception {
		return mockMvc.perform(put("/api/admin/settings").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
			.contentType(MediaType.APPLICATION_JSON)
			.content(settingsJson(codEnabled, max, fee, zones)));
	}

	private static String settingsJson(boolean codEnabled, String max, String fee, String zones) {
		return """
				{"storeName": "Shop Africa", "supportEmail": "contact@shopafrica.sn", "onlinePaymentEnabled": true,
				 "codEnabled": %s, "codMaxAmount": %s, "codFee": %s, "codZones": %s}
				""".formatted(codEnabled, max, fee, zones);
	}

	private ResultActions checkout(String paymentMethod, String city) throws Exception {
		return mockMvc.perform(post("/api/orders").with(awa).contentType(MediaType.APPLICATION_JSON).content("""
				{"paymentMethod": "%s", "delivery": {"addressLine": "Rue 10", "city": "%s", "phone": "+221771234567"}}
				""".formatted(paymentMethod, city)));
	}

}
