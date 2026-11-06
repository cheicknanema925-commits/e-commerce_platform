package com.boutique.support;

import java.time.Instant;

public record TicketResponse(Long id, Long orderId, SupportTicket.Category category, String subject, String message,
		SupportTicket.Status status, String reply, Instant createdAt, Instant repliedAt) {

	static TicketResponse from(SupportTicket ticket) {
		return new TicketResponse(ticket.getId(), ticket.getOrderId(), ticket.getCategory(), ticket.getSubject(),
				ticket.getMessage(), ticket.getStatus(), ticket.getReply(), ticket.getCreatedAt(), ticket.getRepliedAt());
	}

}
