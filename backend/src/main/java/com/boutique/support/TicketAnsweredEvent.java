package com.boutique.support;

public record TicketAnsweredEvent(Long ticketId, String customerEmail, String subject) {
}
