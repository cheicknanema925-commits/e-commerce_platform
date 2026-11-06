package com.boutique.agent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallLimitExceededException;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

import com.boutique.agent.client.ClientTools;

// Un tour de conversation : la boucle de l'agent (modèle ⇄ outils) est menée par le ToolCallingAdvisor du ChatClient ;
// ce service prépare le contexte du tour, diffuse la réponse, puis applique les garde-fous de fin de tour.
@Service
public class AgentChatService {

	private static final Logger log = LoggerFactory.getLogger(AgentChatService.class);

	private static final ZoneId SHOP_ZONE = ZoneId.of("Africa/Dakar");

	// Appel d'outil écrit en texte, n'importe où dans la réponse (vu à l'évaluation en début ET en fin de phrase)
	private static final Pattern RAW_TOOL_CALL = Pattern.compile("\\{\\s*\"name\"\\s*:\\s*\"\\w+\"\\s*,\\s*\"(parameters|arguments)\"");

	// Tournures par lesquelles un modèle annonce une action comme faite
	private static final Pattern CLAIMED_ACTION = Pattern.compile("c'est fait|c’est fait|mise? à jour|"
			+ "(a|ont) été (ajouté|modifié|appliqué|créé|envoyé|transmis|annulé|enregistré|réduit|mis)|"
			+ "j'ai (ajouté|modifié|appliqué|créé|envoyé|transmis|annulé)|j’ai (ajouté|modifié|appliqué|créé|envoyé|transmis|annulé)",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	private final ChatMemory chatMemory;

	private final ProposalService proposalService;

	private final AgentRateLimiter rateLimiter;

	public AgentChatService(ChatMemory chatMemory, ProposalService proposalService, AgentRateLimiter rateLimiter) {
		this.chatMemory = chatMemory;
		this.proposalService = proposalService;
		this.rateLimiter = rateLimiter;
	}

	public Flux<AgentEvent> chat(ChatClient chatClient, AgentKind agent, Long userId, String message) {
		rateLimiter.check(agent, userId);
		TurnContext turn = new TurnContext(userId, agent);
		StringBuilder answer = new StringBuilder();
		Flux<AgentEvent> tokens = chatClient.prompt()
			// Le modèle ne connaît pas la date : on la lui donne (« jusqu'à dimanche » devient une vraie date)
			.system((system) -> system.param("today", today()).param("calendar", calendar()))
			.user(message)
			// Le modèle ne voit jamais ce contexte : seuls les outils le lisent (identité, données du tour)
			.toolContext(Map.of(TurnContext.KEY, turn))
			.advisors((advisor) -> advisor.param(ChatMemory.CONVERSATION_ID, agent.conversationId(userId)))
			.stream()
			.chatResponse()
			.flatMapIterable(AgentChatService::events)
			.doOnNext((event) -> {
				if (event instanceof AgentEvent.Token token) {
					answer.append(token.text());
				}
			});
		return tokens
			.concatWith(Flux.defer(() -> Flux.fromIterable(endOfTurn(turn, answer.toString()))))
			.onErrorResume((ex) -> {
				log.error("Agent {} indisponible pour l'utilisateur {}", agent, userId, ex);
				return Flux.just(new AgentEvent.Failure("L'assistant est momentanément indisponible. Réessayez dans un instant."),
						new AgentEvent.Done());
			});
	}

	private static String today() {
		return dayLabel(LocalDate.now(SHOP_ZONE));
	}

	// Les 14 prochains jours, nommés : un petit modèle LIT « dimanche 8 novembre (2026-11-08) » bien plus sûrement
	// qu'il ne calcule quel jour tombe dimanche
	private static String calendar() {
		LocalDate today = LocalDate.now(SHOP_ZONE);
		StringBuilder calendar = new StringBuilder();
		for (int i = 1; i <= 14; i++) {
			calendar.append(i == 1 ? "" : ", ").append(dayLabel(today.plusDays(i)));
		}
		return calendar.toString();
	}

	private static String dayLabel(LocalDate date) {
		return date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)) + " (" + date + ")";
	}

	// Morceau de réponse → événement. Quand la limite d'appels d'outils est atteinte, Spring AI renvoie un message
	// technique en anglais à la place de la réponse : on le remplace par un avertissement compréhensible
	private static List<AgentEvent> events(ChatResponse response) {
		Generation generation = response.getResult();
		if (generation == null) {
			return List.of();
		}
		if (ToolCallLimitExceededException.FINISH_REASON.equals(generation.getMetadata().getFinishReason())) {
			log.warn("Limite d'appels d'outils atteinte : {}", generation.getOutput().getText());
			return List.of(new AgentEvent.Warning(
					"Je me suis arrêté : cette demande nécessitait trop de recherches. Pouvez-vous la préciser ?"));
		}
		String text = generation.getOutput().getText();
		return (text == null || text.isEmpty()) ? List.of() : List.of(new AgentEvent.Token(text));
	}

	// Garde-fous de fin de tour, appliqués par le code quel que soit le texte produit par le modèle
	private List<AgentEvent> endOfTurn(TurnContext turn, String answer) {
		List<AgentEvent> events = new ArrayList<>();
		if (!turn.products().isEmpty()) {
			// Les fiches affichées viennent de la base, pas du texte du modèle : prix et stock toujours exacts
			events.add(new AgentEvent.Products(turn.products().values().stream().map(ClientTools::summary).toList()));
		}
		if (!turn.steps().isEmpty()) {
			events.add(new AgentEvent.Proposal(proposalService.save(turn)));
		}
		else {
			// Fait vérifié par le code : rien n'a été préparé pendant ce message
			turn.remindersIfNothingProposed().forEach((reminder) -> events.add(new AgentEvent.Warning(reminder)));
			if (RAW_TOOL_CALL.matcher(answer).find()) {
				// Llama 3.2 écrit parfois l'appel d'outil DANS le texte au lieu de l'exécuter
				events.add(new AgentEvent.Warning("L'assistant a répondu de façon technique au lieu d'agir : rien n'a été fait. "
						+ "Reformulez votre demande."));
			}
			else if (turn.remindersIfNothingProposed().isEmpty() && CLAIMED_ACTION.matcher(answer).find()) {
				// « Le prix a été mis à jour » sans aucune carte : on rappelle ce qui fait foi
				events.add(new AgentEvent.Warning(
						"Rien n'a été modifié pendant ce message : seule une carte « Confirmer » peut changer quelque chose."));
			}
		}
		List<BigDecimal> unknown = AmountVerifier.unknownAmounts(answer, turn.knownAmounts());
		if (!unknown.isEmpty()) {
			log.warn("Montants non vérifiés dans la réponse de l'agent {} : {}", turn.agent(), unknown);
			events.add(new AgentEvent.Warning("Montant à vérifier : " + unknown.getFirst().toBigInteger()
					+ " FCFA ne correspond à aucun prix de la boutique. Seuls les prix des fiches affichées font foi."));
		}
		events.add(new AgentEvent.Done());
		return events;
	}

	// Historique affiché à l'ouverture du chat : seulement les messages de l'utilisateur et de l'assistant
	public List<ChatLine> history(AgentKind agent, Long userId) {
		return chatMemory.get(agent.conversationId(userId))
			.stream()
			.filter((m) -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT)
			.filter((m) -> m.getText() != null && !m.getText().isBlank())
			.map(AgentChatService::line)
			.toList();
	}

	public void forget(AgentKind agent, Long userId) {
		chatMemory.clear(agent.conversationId(userId));
	}

	private static ChatLine line(Message message) {
		return new ChatLine(message.getMessageType() == MessageType.USER ? "user" : "assistant", message.getText());
	}

	public record ChatLine(String role, String text) {
	}

}
