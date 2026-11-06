package com.boutique.support;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.common.PageResponse;
import com.boutique.order.OrderNotFoundException;
import com.boutique.order.OrderRepository;
import com.boutique.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class SupportTicketService {

	// Au-delà, le client attend une réponse avant d'en ouvrir d'autres (limite aussi un assistant qui s'emballe)
	static final int MAX_OPEN_TICKETS = 5;

	private final SupportTicketRepository ticketRepository;

	private final OrderRepository orderRepository;

	private final UserRepository userRepository;

	private final ApplicationEventPublisher events;

	public SupportTicketService(SupportTicketRepository ticketRepository, OrderRepository orderRepository,
			UserRepository userRepository, ApplicationEventPublisher events) {
		this.ticketRepository = ticketRepository;
		this.orderRepository = orderRepository;
		this.userRepository = userRepository;
		this.events = events;
	}

	// Toutes les règles, sans rien enregistrer : l'assistant les vérifie AVANT de proposer l'action
	public void checkCanOpen(Long userId, TicketRequest request) {
		if (request.category() == null) {
			throw new InvalidTicketException("Choisissez le type de demande.");
		}
		if (isBlankOrLonger(request.subject(), 120) || isBlankOrLonger(request.message(), 2000)) {
			throw new InvalidTicketException("Objet (120 caractères max) et message (2000 max) sont obligatoires.");
		}
		// 404 pour la commande d'un autre client : on ne révèle pas qu'elle existe
		if (request.orderId() != null && orderRepository.findByIdAndUserId(request.orderId(), userId).isEmpty()) {
			throw new OrderNotFoundException(request.orderId());
		}
		if (ticketRepository.countByUserIdAndStatus(userId, SupportTicket.Status.OPEN) >= MAX_OPEN_TICKETS) {
			throw new TooManyOpenTicketsException(MAX_OPEN_TICKETS);
		}
	}

	@Transactional
	public TicketResponse open(Long userId, TicketRequest request) {
		checkCanOpen(userId, request);
		SupportTicket ticket = new SupportTicket(userRepository.getReferenceById(userId), request.orderId(),
				request.category(), request.subject().strip(), request.message().strip());
		return TicketResponse.from(ticketRepository.save(ticket));
	}

	public List<TicketResponse> ticketsOf(Long userId) {
		return ticketRepository.findTop20ByUserIdOrderByIdDesc(userId).stream().map(TicketResponse::from).toList();
	}

	public PageResponse<AdminTicketResponse> findAll(SupportTicket.Status status, Pageable pageable) {
		return PageResponse.from((status == null ? ticketRepository.findAllBy(pageable)
				: ticketRepository.findByStatus(status, pageable))
			.map(AdminTicketResponse::from));
	}

	// Le client est prévenu APRÈS le commit (TicketNotificationListener) : jamais pour une réponse annulée
	@Transactional
	public AdminTicketResponse reply(Long ticketId, String reply) {
		SupportTicket ticket = getTicket(ticketId);
		ticket.answer(reply.strip(), Instant.now());
		events.publishEvent(new TicketAnsweredEvent(ticket.getId(), ticket.getUser().getEmail(), ticket.getSubject()));
		return AdminTicketResponse.from(ticket);
	}

	@Transactional
	public AdminTicketResponse close(Long ticketId) {
		SupportTicket ticket = getTicket(ticketId);
		ticket.close();
		return AdminTicketResponse.from(ticket);
	}

	private SupportTicket getTicket(Long ticketId) {
		return ticketRepository.findWithUserById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
	}

	private static boolean isBlankOrLonger(String value, int max) {
		return value == null || value.isBlank() || value.strip().length() > max;
	}

}
