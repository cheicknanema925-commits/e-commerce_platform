package com.boutique.agent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.ai.chat.model.ToolContext;

import com.boutique.catalog.ProductResponse;

// État d'UN message (un « tour ») : qui parle, ce que les outils ont lu en base, ce qu'ils ont préparé.
// Il voyage dans le ToolContext, que le modèle ne voit jamais : l'identité ne peut pas venir de lui.
// Les outils peuvent tourner sur plusieurs threads : collections concurrentes.
public final class TurnContext {

	public static final String KEY = "boutique.turn";

	private final Long userId;

	private final AgentKind agent;

	private final Map<Long, ProductResponse> products = new ConcurrentHashMap<>();

	// Montants réellement lus en base pendant ce tour (prix, totaux) : la réponse ne doit citer qu'eux
	private final Set<BigDecimal> knownAmounts = ConcurrentHashMap.newKeySet();

	private final CopyOnWriteArrayList<ProposalStep> steps = new CopyOnWriteArrayList<>();

	// Messages affichés si le tour se termine SANS action proposée (ex. : un aperçu de prix pris pour un changement)
	private final Set<String> reminders = ConcurrentHashMap.newKeySet();

	public TurnContext(Long userId, AgentKind agent) {
		this.userId = userId;
		this.agent = agent;
	}

	public static TurnContext from(ToolContext toolContext) {
		if (toolContext == null || !(toolContext.getContext().get(KEY) instanceof TurnContext turn)) {
			throw new IllegalStateException("Outil appelé sans contexte de tour");
		}
		return turn;
	}

	public void seeProduct(ProductResponse product) {
		products.put(product.id(), product);
		knownAmounts.add(product.price());
		if (product.compareAtPrice() != null) {
			knownAmounts.add(product.compareAtPrice());
		}
	}

	public void seeAmount(BigDecimal amount) {
		if (amount != null) {
			knownAmounts.add(amount);
		}
	}

	// Un modèle qui appelle deux fois le même outil ne doit pas doubler l'action (2 × 2 sandales en un clic)
	public void propose(ProposalStep step) {
		steps.addIfAbsent(step);
	}

	public void remindIfNothingProposed(String reminder) {
		reminders.add(reminder);
	}

	public Long userId() {
		return userId;
	}

	public AgentKind agent() {
		return agent;
	}

	public Map<Long, ProductResponse> products() {
		return Map.copyOf(products);
	}

	public Set<BigDecimal> knownAmounts() {
		return Set.copyOf(knownAmounts);
	}

	public List<ProposalStep> steps() {
		return List.copyOf(steps);
	}

	public Set<String> remindersIfNothingProposed() {
		return Set.copyOf(reminders);
	}

}
