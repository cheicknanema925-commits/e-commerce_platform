package com.boutique.agent.client;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

import com.boutique.agent.AgentChatService;
import com.boutique.agent.AgentChatService.ChatLine;
import com.boutique.agent.AgentEvent;
import com.boutique.agent.AgentKind;
import com.boutique.agent.ProposalResult;
import com.boutique.agent.ProposalService;
import com.boutique.user.CurrentUserResolver;

// Assistant client : réservé aux utilisateurs connectés (règle « tout le reste authentifié » de SecurityConfig).
// L'identité vient du jeton, jamais du message ni du modèle.
@RestController
@RequestMapping("/api/agent")
public class ClientAgentController {

	private final ChatClient clientAgent;

	private final AgentChatService chatService;

	private final ProposalService proposalService;

	private final CurrentUserResolver currentUserResolver;

	public ClientAgentController(@Qualifier("clientAgent") ChatClient clientAgent, AgentChatService chatService,
			ProposalService proposalService, CurrentUserResolver currentUserResolver) {
		this.clientAgent = clientAgent;
		this.chatService = chatService;
		this.proposalService = proposalService;
		this.currentUserResolver = currentUserResolver;
	}

	// Réponse en streaming (Server-Sent Events) : le texte s'affiche au fur et à mesure qu'il est produit
	@PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<AgentEvent>> chat(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChatRequest request) {
		return chatService.chat(clientAgent, AgentKind.CLIENT, currentUserResolver.userId(jwt), request.message())
			.map((event) -> ServerSentEvent.builder(event).event(event.type()).build());
	}

	@GetMapping("/conversation")
	public List<ChatLine> history(@AuthenticationPrincipal Jwt jwt) {
		return chatService.history(AgentKind.CLIENT, currentUserResolver.userId(jwt));
	}

	@DeleteMapping("/conversation")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void forget(@AuthenticationPrincipal Jwt jwt) {
		chatService.forget(AgentKind.CLIENT, currentUserResolver.userId(jwt));
	}

	// La confirmation est un CLIC de l'utilisateur : le modèle n'a aucun moyen d'appeler cette route
	@PostMapping("/proposals/{id}/confirm")
	public ProposalResult confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return proposalService.confirm(currentUserResolver.userId(jwt), AgentKind.CLIENT, id);
	}

	@PostMapping("/proposals/{id}/cancel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		proposalService.cancel(currentUserResolver.userId(jwt), AgentKind.CLIENT, id);
	}

	public record ChatRequest(@NotBlank @Size(max = 1000) String message) {
	}

}
