import { apiGet, apiRequest } from './client.ts'
import type { Product, StoreSettings } from './types.ts'

// Favoris du client connecté (l'API l'identifie par son jeton)
export function fetchFavorites(): Promise<Product[]> {
  return apiGet('/favorites')
}

export function fetchFavoriteIds(): Promise<number[]> {
  return apiGet('/favorites/ids')
}

export function setFavorite(input: { productId: number; favorite: boolean }): Promise<void> {
  return apiRequest(input.favorite ? 'PUT' : 'DELETE', `/favorites/${input.productId}`)
}

// Infolettre : même réponse que l'adresse soit nouvelle ou déjà inscrite
export function subscribeToNewsletter(email: string): Promise<{ message: string }> {
  return apiRequest('POST', '/newsletter/subscriptions', { body: { email } })
}

export function unsubscribeFromNewsletter(token: string): Promise<void> {
  return apiRequest('POST', '/newsletter/unsubscribe', { body: { token } })
}

export function fetchStoreSettings(): Promise<StoreSettings> {
  return apiGet('/store/settings')
}
