import { apiGet, apiRequest } from './client.ts'
import type { Cart } from './types.ts'

// Aucun prix n'est envoyé : l'API lit toujours les prix en base

export function fetchCart(): Promise<Cart> {
  return apiGet('/cart')
}

export function addToCart(item: { productId: number; quantity: number }): Promise<Cart> {
  return apiRequest('POST', '/cart/items', { body: item })
}

export function changeQuantity(item: { productId: number; quantity: number }): Promise<Cart> {
  return apiRequest('PUT', `/cart/items/${item.productId}`, { body: { quantity: item.quantity } })
}

export function removeFromCart(productId: number): Promise<Cart> {
  return apiRequest('DELETE', `/cart/items/${productId}`)
}
