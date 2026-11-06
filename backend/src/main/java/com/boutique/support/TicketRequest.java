package com.boutique.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// orderId facultatif : la commande doit appartenir au client (vérifié par le service, jamais cru sur parole)
public record TicketRequest(@NotNull SupportTicket.Category category, @NotBlank @Size(max = 120) String subject,
		@NotBlank @Size(max = 2000) String message, Long orderId) {
}
