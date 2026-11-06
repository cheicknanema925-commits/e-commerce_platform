package com.boutique.agent.client;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.boutique.agent.AgentKind;
import com.boutique.agent.StepHandler;
import com.boutique.support.SupportTicket;
import com.boutique.support.SupportTicketService;
import com.boutique.support.TicketRequest;

@Component
class OpenTicketHandler implements StepHandler {

	static final String TYPE = "OPEN_SUPPORT_TICKET";

	private final SupportTicketService ticketService;

	OpenTicketHandler(SupportTicketService ticketService) {
		this.ticketService = ticketService;
	}

	@Override
	public String type() {
		return TYPE;
	}

	@Override
	public AgentKind agent() {
		return AgentKind.CLIENT;
	}

	// Les règles sont revérifiées par le service : la situation a pu changer depuis la proposition
	@Override
	public String execute(Long userId, Map<String, Object> params) {
		var ticket = ticketService.open(userId, new TicketRequest(
				SupportTicket.Category.valueOf(StepHandler.stringParam(params, "category")),
				StepHandler.stringParam(params, "subject"), StepHandler.stringParam(params, "message"),
				StepHandler.optionalLongParam(params, "orderId")));
		return "demande n° " + ticket.id() + " envoyée au service client";
	}

}
