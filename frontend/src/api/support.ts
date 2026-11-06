import { apiGet, apiRequest } from './client.ts'
import type { AdminTicket, Page, PreOrder, PreOrderDemand, PriceChange, SupportTicket, TicketInput, TicketStatus } from './types.ts'

// Demandes au service client du client connecté (l'API l'identifie par son jeton)
export function fetchMyTickets(): Promise<SupportTicket[]> {
  return apiGet('/support/tickets')
}

export function openTicket(input: TicketInput): Promise<SupportTicket> {
  return apiRequest('POST', '/support/tickets', { body: input })
}

// Pré-commandes : « prévenez-moi quand ce produit revient en stock »
export function fetchMyPreOrders(): Promise<PreOrder[]> {
  return apiGet('/preorders')
}

export function preOrder(input: { productId: number; quantity: number }): Promise<PreOrder> {
  return apiRequest('POST', '/preorders', { body: input })
}

export function cancelPreOrder(id: number): Promise<void> {
  return apiRequest('DELETE', `/preorders/${id}`)
}

// Back-office
export function fetchAdminTickets(status: TicketStatus | undefined, page: number): Promise<Page<AdminTicket>> {
  return apiGet('/admin/support/tickets', { status, page })
}

export function replyToTicket(input: { id: number; reply: string }): Promise<AdminTicket> {
  return apiRequest('PUT', `/admin/support/tickets/${input.id}/reply`, { body: { reply: input.reply } })
}

export function closeTicket(id: number): Promise<AdminTicket> {
  return apiRequest('PUT', `/admin/support/tickets/${id}/close`)
}

export function fetchPreOrderDemand(): Promise<PreOrderDemand[]> {
  return apiGet('/admin/preorders')
}

export function fetchPriceChanges(page: number): Promise<Page<PriceChange>> {
  return apiGet('/admin/price-changes', { page })
}
