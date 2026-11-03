import { apiGet, apiRequest } from './client.ts'
import type { Delivery, DeliveryAddress, Order, PaymentMethod, PaymentSession } from './types.ts'

export function checkout(request: { delivery: DeliveryAddress; paymentMethod: PaymentMethod }): Promise<Order> {
  return apiRequest('POST', '/orders', { body: request })
}

export function fetchMyOrders(): Promise<Order[]> {
  return apiGet('/orders/me')
}

export function fetchOrder(id: number): Promise<Order> {
  return apiGet(`/orders/${id}`)
}

export function fetchDelivery(orderId: number): Promise<Delivery> {
  return apiGet(`/orders/${orderId}/delivery`)
}

export function cancelOrder(id: number): Promise<Order> {
  return apiRequest('POST', `/orders/${id}/cancel`)
}

// Renvoie l'adresse de la page de paiement HÉBERGÉE par le fournisseur (on n'y saisit rien chez nous)
export function startPayment(orderId: number): Promise<PaymentSession> {
  return apiRequest('POST', `/orders/${orderId}/payment`)
}

// Développement uniquement : imite ce que le client fait sur la page du fournisseur, puis son webhook signé
export function simulatePayment(reference: string, outcome: 'paye' | 'annule'): Promise<void> {
  return apiRequest('POST', `/dev/fake-payments/${encodeURIComponent(reference)}/${outcome}`)
}
