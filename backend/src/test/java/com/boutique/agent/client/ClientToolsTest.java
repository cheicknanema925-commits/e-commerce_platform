package com.boutique.agent.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import com.boutique.agent.AgentKind;
import com.boutique.agent.ProductIndexer;
import com.boutique.agent.ToolCallLogRepository;
import com.boutique.agent.ToolRunner;
import com.boutique.agent.TurnContext;
import com.boutique.cart.CartService;
import com.boutique.catalog.ProductResponse;
import com.boutique.catalog.ProductService;
import com.boutique.common.PageResponse;
import com.boutique.order.OrderService;
import com.boutique.preorder.PreOrderService;
import com.boutique.review.ReviewService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClientToolsTest {

	private ProductService productService;

	private OrderService orderService;

	private CartService cartService;

	private ProductIndexer productIndexer;

	private ClientTools tools;

	private final TurnContext turn = new TurnContext(42L, AgentKind.CLIENT);

	private final ToolContext context = new ToolContext(Map.of(TurnContext.KEY, turn));

	@BeforeEach
	void setUp() {
		productService = mock(ProductService.class);
		orderService = mock(OrderService.class);
		cartService = mock(CartService.class);
		productIndexer = mock(ProductIndexer.class);
		tools = new ClientTools(productService, mock(ReviewService.class), orderService, cartService,
				new ToolRunner(mock(ToolCallLogRepository.class)), new ProductFinder(productService, productIndexer),
				mock(PreOrderService.class));
		when(productService.findAll(isNull(), anyString(), eq(false), any())).thenReturn(page());
	}

	@Test
	void keywordResultsComeFirstThenSemanticOnesWithoutDuplicates() {
		when(productService.findAll(isNull(), eq("sandales"), eq(false), any())).thenReturn(page(product(2L, "Sandales cuir")));
		when(productIndexer.similarProducts("sandales", ClientTools.MAX_RESULTS))
			.thenReturn(List.of(product(2L, "Sandales cuir"), product(5L, "Babouches")));

		Object result = tools.searchProducts("sandales", context);

		assertThat(ids(result)).containsExactly(2L, 5L);
		// Les fiches et les prix du tour viennent de la base, pas du texte du modèle
		assertThat(turn.products()).containsOnlyKeys(2L, 5L);
		assertThat(turn.knownAmounts()).contains(new BigDecimal("8000"));
	}

	@Test
	void misspelledQueryIsSearchedWordByWord() {
		// Un petit modèle écrit « sacsà dos » : la phrase ne donne rien, le mot « dos » trouve le sac
		when(productService.findAll(isNull(), eq("dos"), eq(false), any())).thenReturn(page(product(9L, "Sac à dos")));

		Object result = tools.searchProducts("sacsà dos", context);

		assertThat(ids(result)).containsExactly(9L);
	}

	@Test
	void emptyFieldsAreLeftOutSoTheModelDoesNotTalkAboutThem() {
		assertThat(ClientTools.summary(product(2L, "Sandales cuir"))).doesNotContainKeys("marque", "prixAvantRemiseFcfa")
			.containsEntry("prixFcfa", new BigDecimal("8000").toBigInteger());
	}

	@Test
	void ordersAreAlwaysThoseOfTheUserOfTheToken() {
		when(orderService.findOrdersOfUser(42L)).thenReturn(List.of());

		tools.getMyOrderStatus(null, context);

		// L'identité vient du TurnContext (jeton), le modèle n'a aucun paramètre pour la changer
		verify(orderService).findOrdersOfUser(42L);
	}

	@Test
	void addToCartIsOnlyProposedNeverExecuted() {
		when(productService.findById(2L)).thenReturn(product(2L, "Sandales cuir"));

		tools.proposeAddToCart("2", 2, context);

		assertThat(turn.steps()).singleElement()
			.satisfies((step) -> assertThat(step.type()).isEqualTo(AddToCartHandler.TYPE))
			.satisfies((step) -> assertThat(step.params()).containsEntry("productId", 2L).containsEntry("quantity", 2));
		verifyNoInteractions(cartService);
	}

	@Test
	void sameActionAskedTwiceInOneMessageIsProposedOnce() {
		// Sinon un seul clic ajouterait 2 × 2 sandales
		when(productService.findById(2L)).thenReturn(product(2L, "Sandales cuir"));

		tools.proposeAddToCart("2", 2, context);
		tools.proposeAddToCart("2", 2, context);

		assertThat(turn.steps()).hasSize(1);
	}

	@Test
	void impossibleQuantitiesAreRefusedWithoutProposal() {
		when(productService.findById(2L)).thenReturn(product(2L, "Sandales cuir"));

		assertThat(tools.proposeAddToCart("2", 0, context)).isEqualTo(Map.of("erreur", "La quantité doit être comprise entre 1 et 99."));
		assertThat(tools.proposeAddToCart("2", 10, context)).isEqualTo(Map.of("erreur", "Stock insuffisant : 9 disponible(s)."));
		assertThat(turn.steps()).isEmpty();
		verifyNoInteractions(cartService);
	}

	// Identifiants des fiches renvoyées au modèle
	private static List<Object> ids(Object result) {
		return ((List<?>) result).stream().<Object>map((p) -> ((Map<?, ?>) p).get("id")).toList();
	}

	@Test
	void productIsFoundByTheNameTheCustomerUsed() {
		when(productService.findAll(isNull(), eq("sandales cuir"), eq(false), any())).thenReturn(page(product(2L, "Sandales cuir")));

		tools.proposeAddToCart("sandales cuir", 1, context);

		assertThat(turn.steps()).singleElement().satisfies((step) -> assertThat(step.params()).containsEntry("productId", 2L));
	}

	@Test
	void ambiguousNameReturnsTheCandidatesInsteadOfGuessing() {
		when(productService.findAll(isNull(), eq("pagne"), eq(false), any()))
			.thenReturn(page(product(3L, "Pagne wax"), product(5L, "Pagne kente")));

		Object result = tools.proposeAddToCart("pagne", 1, context);

		assertThat(result).asString().contains("Plusieurs produits", "Pagne wax", "Pagne kente");
		assertThat(turn.steps()).isEmpty();
		// Les candidats s'affichent en fiches : le client choisit sur des données de la base
		assertThat(turn.products()).containsOnlyKeys(3L, 5L);
	}

	@Test
	void productFoundOnlyBySenseIsNeverPickedAutomatically() {
		when(productIndexer.similarProducts("chaussures de fête", ClientTools.MAX_RESULTS))
			.thenReturn(List.of(product(2L, "Sandales cuir")));

		assertThat(tools.proposeAddToCart("chaussures de fête", 1, context)).asString().contains("Sandales cuir");
		assertThat(turn.steps()).isEmpty();
	}

	@SafeVarargs
	private static PageResponse<ProductResponse> page(ProductResponse... products) {
		return new PageResponse<>(List.of(products), 0, ClientTools.MAX_RESULTS, products.length, 1);
	}

	private static ProductResponse product(Long id, String name) {
		return new ProductResponse(id, name, null, new BigDecimal("8000"), 9, 1L, "Mode", List.of(), null, "SHA-" + id,
				null, null);
	}

}
