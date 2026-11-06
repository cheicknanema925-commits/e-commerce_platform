package com.boutique.agent.client;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.boutique.agent.ProposalStep;
import com.boutique.agent.ToolRunner;
import com.boutique.agent.TurnContext;
import com.boutique.order.OrderNotFoundException;
import com.boutique.order.OrderService;
import com.boutique.preorder.PreOrderService;
import com.boutique.support.SupportTicket;
import com.boutique.support.SupportTicketService;
import com.boutique.support.TicketRequest;

// Outils « service client » de l'assistant CLIENT : demandes au SAV et pré-commandes.
// Mêmes règles que ClientTools : identité lue dans le TurnContext, actions seulement proposées.
@Component
public class CustomerServiceTools {

	private final SupportTicketService ticketService;

	private final PreOrderService preOrderService;

	private final OrderService orderService;

	private final ToolRunner runner;

	private final ProductFinder productFinder;

	public CustomerServiceTools(SupportTicketService ticketService, PreOrderService preOrderService,
			OrderService orderService, ToolRunner runner, ProductFinder productFinder) {
		this.ticketService = ticketService;
		this.preOrderService = preOrderService;
		this.orderService = orderService;
		this.runner = runner;
		this.productFinder = productFinder;
	}

	@Tool(description = "Demandes du client connecté au service client (20 dernières) avec leur statut et la réponse reçue.")
	public Object getMyTickets(ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "getMyTickets", "", () -> ticketService.ticketsOf(turn.userId()).stream().map((ticket) -> {
			Map<String, Object> view = new LinkedHashMap<>();
			view.put("numero", ticket.id());
			view.put("objet", ticket.subject());
			view.put("statut", ticket.status());
			if (ticket.orderId() != null) {
				view.put("commande", ticket.orderId());
			}
			if (ticket.reply() != null) {
				view.put("reponseDuServiceClient", ticket.reply());
			}
			return view;
		}).toList());
	}

	@Tool(description = "PRÉPARE une demande au service client (colis abîmé, retard, remboursement, question). "
			+ "Rien n'est envoyé tant que le client n'a pas cliqué sur « Confirmer ». "
			+ "Le message résume la demande du client avec ses mots, sans rien inventer.")
	public Object proposeSupportTicket(
			@ToolParam(description = "Type : ORDER, DELIVERY, PAYMENT, PRODUCT ou OTHER") String category,
			@ToolParam(description = "Objet court (120 caractères maximum)") String subject,
			@ToolParam(description = "Message pour le service client (2000 caractères maximum)") String message,
			@ToolParam(description = "Numéro de la commande concernée (facultatif)", required = false) Long orderId,
			ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		String arguments = category + " | " + subject + " | commande " + orderId;
		if (orderId != null && !isOrderOf(turn.userId(), orderId)) {
			// Un petit modèle invente parfois un numéro : on lui dit comment s'en sortir au lieu d'un simple refus
			return runner.refuse(turn, "proposeSupportTicket", arguments, "Le client n'a pas de commande n° " + orderId
					+ ". Appelle getMyOrderStatus sans numéro pour voir ses commandes, ou prépare la demande sans numéro.");
		}
		return runner.run(turn, "proposeSupportTicket", arguments, () -> {
			TicketRequest request = new TicketRequest(category(category), subject, message, orderId);
			// Mêmes règles qu'à l'enregistrement : commande du client (IDOR), longueurs, 5 demandes ouvertes maximum
			ticketService.checkCanOpen(turn.userId(), request);
			Map<String, Object> params = new HashMap<>();
			params.put("category", request.category().name());
			params.put("subject", subject.strip());
			params.put("message", message.strip());
			params.put("orderId", orderId);
			// Le client voit EXACTEMENT ce qui sera envoyé avant de confirmer
			turn.propose(new ProposalStep(OpenTicketHandler.TYPE, params, "Envoyer au service client"
					+ (orderId != null ? " (commande n° " + orderId + ")" : "") + " : « " + subject.strip() + " » — "
					+ excerpt(message.strip())));
			return "Demande préparée : le client doit la relire puis cliquer sur « Confirmer ».";
		});
	}

	@Tool(description = "Pré-commandes du client connecté : produits en rupture qu'il attend.")
	public Object getMyPreOrders(ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "getMyPreOrders", "", () -> preOrderService.preOrdersOf(turn.userId())
			.stream()
			.map((preOrder) -> Map.of("produit", preOrder.productName(), "quantite", preOrder.quantity(), "statut",
					preOrder.status()))
			.toList());
	}

	@Tool(description = "PRÉPARE une pré-commande pour un produit EN RUPTURE DE STOCK : le client sera prévenu à son "
			+ "retour. Rien n'est payé ni réservé. Pour un produit disponible, utilise proposeAddToCart.")
	public Object proposePreOrder(@ToolParam(description = ClientTools.PRODUCT_PARAM) String product,
			@ToolParam(description = "Quantité souhaitée (1 à 99)") Integer quantity, ToolContext toolContext) {
		TurnContext turn = TurnContext.from(toolContext);
		return runner.run(turn, "proposePreOrder", product + " x " + quantity, () -> ClientTools.withProduct(product, turn,
				productFinder, (found) -> {
			int wanted = (quantity == null) ? 1 : quantity;
			String productName = preOrderService.checkCanPreOrder(found.id(), wanted);
			turn.propose(new ProposalStep(PreOrderHandler.TYPE, Map.of("productId", found.id(), "quantity", wanted),
					PreOrderHandler.summary(productName, wanted)));
			return "Pré-commande préparée : le client doit la confirmer avec le bouton « Confirmer ».";
		}));
	}

	private boolean isOrderOf(Long userId, Long orderId) {
		try {
			orderService.findOrderOfUser(userId, orderId);
			return true;
		}
		catch (OrderNotFoundException ex) {
			return false;
		}
	}

	private static SupportTicket.Category category(String value) {
		try {
			return SupportTicket.Category.valueOf(value == null ? "" : value.strip().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			// Un petit modèle peut écrire « livraison » : la demande part quand même, classée « autre »
			return SupportTicket.Category.OTHER;
		}
	}

	private static String excerpt(String text) {
		return text.length() <= 300 ? "« " + text + " »" : "« " + text.substring(0, 300) + "… »";
	}

}
