package com.boutique.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.common.PageResponse;

// /api/admin/** : réservé au rôle ADMIN par SecurityConfig
@RestController
@RequestMapping("/api/admin/support/tickets")
public class AdminSupportTicketController {

	private final SupportTicketService ticketService;

	public AdminSupportTicketController(SupportTicketService ticketService) {
		this.ticketService = ticketService;
	}

	@GetMapping
	public PageResponse<AdminTicketResponse> tickets(@RequestParam(required = false) SupportTicket.Status status,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ticketService.findAll(status, pageable);
	}

	@PutMapping("/{id}/reply")
	public AdminTicketResponse reply(@PathVariable Long id, @Valid @RequestBody ReplyRequest request) {
		return ticketService.reply(id, request.reply());
	}

	@PutMapping("/{id}/close")
	public AdminTicketResponse close(@PathVariable Long id) {
		return ticketService.close(id);
	}

	public record ReplyRequest(@NotBlank @Size(max = 2000) String reply) {
	}

}
