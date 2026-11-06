package com.boutique.support;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.user.CurrentUserResolver;

@RestController
@RequestMapping("/api/support/tickets")
public class SupportTicketController {

	private final SupportTicketService ticketService;

	private final CurrentUserResolver currentUserResolver;

	public SupportTicketController(SupportTicketService ticketService, CurrentUserResolver currentUserResolver) {
		this.ticketService = ticketService;
		this.currentUserResolver = currentUserResolver;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TicketResponse open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TicketRequest request) {
		return ticketService.open(currentUserResolver.userId(jwt), request);
	}

	@GetMapping
	public List<TicketResponse> mine(@AuthenticationPrincipal Jwt jwt) {
		return ticketService.ticketsOf(currentUserResolver.userId(jwt));
	}

}
