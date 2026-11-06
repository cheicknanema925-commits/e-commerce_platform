package com.boutique.agent;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import com.boutique.agent.ProposalResult.StepResult;
import com.boutique.common.BadRequestException;
import com.boutique.common.ConflictException;
import com.boutique.common.NotFoundException;

// Cycle de vie des actions proposées : enregistrement, confirmation (exécution étape par étape), annulation.
// La confirmation vient d'un CLIC de l'utilisateur, jamais du modèle : le modèle ne peut que proposer.
@Service
public class ProposalService {

	private static final Logger log = LoggerFactory.getLogger(ProposalService.class);

	private final AgentProposalRepository repository;

	private final Map<String, StepHandler> handlers;

	private final ChatMemory chatMemory;

	private final JsonMapper jsonMapper;

	private final AgentProperties properties;

	private final Clock clock;

	public ProposalService(AgentProposalRepository repository, List<StepHandler> handlers, ChatMemory chatMemory,
			JsonMapper jsonMapper, AgentProperties properties) {
		this.repository = repository;
		this.handlers = handlers.stream().collect(Collectors.toMap(StepHandler::type, Function.identity()));
		this.chatMemory = chatMemory;
		this.jsonMapper = jsonMapper;
		this.properties = properties;
		this.clock = Clock.systemUTC();
	}

	// Fin d'un tour : si les outils ont préparé des actions, on les enregistre EN ATTENTE (rien n'est exécuté)
	@Transactional
	public ProposalView save(TurnContext turn) {
		List<ProposalStep> steps = turn.steps();
		Instant now = clock.instant();
		AgentProposal proposal = repository.save(new AgentProposal(turn.userId(), turn.agent(),
				jsonMapper.writeValueAsString(steps), now, now.plus(properties.proposalTtl())));
		return new ProposalView(proposal.getId(), steps.stream().map(ProposalStep::summary).toList(),
				proposal.getExpiresAt());
	}

	// Pas de @Transactional ici : chaque étape s'exécute dans la transaction de son propre service, une étape en
	// échec n'annule pas les autres (le client sait exactement ce qui a été fait)
	public ProposalResult confirm(Long userId, AgentKind agent, UUID id) {
		AgentProposal proposal = repository.findByIdAndUserIdAndAgent(id, userId, agent)
			.orElseThrow(ProposalNotFoundException::new);
		if (repository.claim(id, AgentProposal.Status.CONFIRMED, clock.instant()) == 0) {
			throw new ProposalNotPendingException();
		}
		List<StepResult> results = new ArrayList<>();
		for (ProposalStep step : readSteps(proposal)) {
			results.add(execute(userId, agent, step));
		}
		ProposalResult result = new ProposalResult(List.copyOf(results));
		AgentProposal confirmed = repository.findById(id).orElseThrow(ProposalNotFoundException::new);
		confirmed.finish(result.describe(), clock.instant());
		repository.save(confirmed);
		// L'agent l'apprend au prochain message (sinon il croirait l'action encore en attente)
		chatMemory.add(agent.conversationId(userId),
				new AssistantMessage("Actions confirmées par l'utilisateur et exécutées par la boutique :\n" + result.describe()));
		return result;
	}

	@Transactional
	public void cancel(Long userId, AgentKind agent, UUID id) {
		repository.findByIdAndUserIdAndAgent(id, userId, agent).orElseThrow(ProposalNotFoundException::new);
		if (repository.claim(id, AgentProposal.Status.CANCELLED, clock.instant()) == 0) {
			throw new ProposalNotPendingException();
		}
	}

	private StepResult execute(Long userId, AgentKind agent, ProposalStep step) {
		StepHandler handler = handlers.get(step.type());
		if (handler == null || handler.agent() != agent) {
			return new StepResult(step.summary(), false, "action non autorisée");
		}
		try {
			return new StepResult(step.summary(), true, handler.execute(userId, step.params()));
		}
		catch (NotFoundException | ConflictException | BadRequestException ex) {
			// Refus métier (stock épuisé entre-temps, limite atteinte…) : son message est fait pour le client
			return new StepResult(step.summary(), false, ex.getMessage());
		}
		catch (RuntimeException ex) {
			// Erreur technique : le détail reste dans les logs, jamais dans la réponse
			log.error("Étape {} en échec pour l'utilisateur {}", step.type(), userId, ex);
			return new StepResult(step.summary(), false, "action impossible pour le moment, réessayez plus tard");
		}
	}

	private List<ProposalStep> readSteps(AgentProposal proposal) {
		return jsonMapper.readValue(proposal.getSteps(), new TypeReference<List<ProposalStep>>() {
		});
	}

}
