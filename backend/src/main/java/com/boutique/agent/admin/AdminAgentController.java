package com.boutique.agent.admin;

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

// Assistant admin : /api/admin/** est réservé au rôle ADMIN par SecurityConfig. Conversation, mémoire et propositions
// séparées de l'assistant client (AgentKind.ADMIN) : une proposition admin ne se confirme pas par la route client.
@RestController
@RequestMapping("/api/admin/agent")
public class AdminAgentController {

	private final ChatClient adminAgent;

	private final AgentChatService chatService;

	private final ProposalService proposalService;

	private final CurrentUserResolver currentUserResolver;

	public AdminAgentController(@Qualifier("adminAgent") ChatClient adminAgent, AgentChatService chatService,
			ProposalService proposalService, CurrentUserResolver currentUserResolver) {
		this.adminAgent = adminAgent;
		this.chatService = chatService;
		this.proposalService = proposalService;
		this.currentUserResolver = currentUserResolver;
	}

	// Réponse en streaming (Server-Sent Events) : le texte s'affiche au fur et à mesure qu'il est produit
	@PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<AgentEvent>> chat(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChatRequest request) {
		return chatService.chat(adminAgent, AgentKind.ADMIN, currentUserResolver.userId(jwt), request.message())
			.map((event) -> ServerSentEvent.builder(event).event(event.type()).build());
	}

	@GetMapping("/conversation")
	public List<ChatLine> history(@AuthenticationPrincipal Jwt jwt) {
		return chatService.history(AgentKind.ADMIN, currentUserResolver.userId(jwt));
	}

	@DeleteMapping("/conversation")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void forget(@AuthenticationPrincipal Jwt jwt) {
		chatService.forget(AgentKind.ADMIN, currentUserResolver.userId(jwt));
	}

	// La confirmation est un CLIC de l'utilisateur : le modèle n'a aucun moyen d'appeler cette route
	@PostMapping("/proposals/{id}/confirm")
	public ProposalResult confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return proposalService.confirm(currentUserResolver.userId(jwt), AgentKind.ADMIN, id);
	}

	@PostMapping("/proposals/{id}/cancel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		proposalService.cancel(currentUserResolver.userId(jwt), AgentKind.ADMIN, id);
	}

	public record ChatRequest(@NotBlank @Size(max = 1000) String message) {
	}

}
