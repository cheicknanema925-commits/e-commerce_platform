package com.boutique.support;

// Back-office : le ticket et qui l'a ouvert
public record AdminTicketResponse(TicketResponse ticket, String customerName, String customerEmail) {

	static AdminTicketResponse from(SupportTicket ticket) {
		return new AdminTicketResponse(TicketResponse.from(ticket), ticket.getUser().getFullName(),
				ticket.getUser().getEmail());
	}

}
