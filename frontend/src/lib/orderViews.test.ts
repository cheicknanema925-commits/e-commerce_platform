import { describe, expect, it } from 'vitest'
import type { AdminStats } from '../api/types.ts'
import { findOrderView, orderViewCount, pendingActionCount } from './orderViews.ts'

const stats = {
  orderCount: 12,
  awaitingConfirmation: 2,
  ordersByStatus: [
    { status: 'CREATED', count: 5 },
    { status: 'PAID', count: 3 },
    { status: 'SHIPPED', count: 1 },
    { status: 'DELIVERED', count: 3 },
  ],
} as AdminStats

describe('orderViewCount', () => {
  it('sépare les commandes CREATED en espèces à confirmer et en ligne non payées', () => {
    expect(orderViewCount(findOrderView('a-confirmer'), stats)).toBe(2)
    expect(orderViewCount(findOrderView('attente-paiement'), stats)).toBe(3)
  })

  it('compte 0 pour un statut absent des statistiques', () => {
    expect(orderViewCount(findOrderView('annulees'), stats)).toBe(0)
  })
})

describe('pendingActionCount', () => {
  it('additionne seulement ce qui attend l’admin : à confirmer, à préparer, en préparation, à livrer', () => {
    expect(pendingActionCount(stats)).toBe(2 + 3 + 0 + 1)
  })
})

describe('findOrderView', () => {
  it('revient à « Toutes » pour une valeur inconnue dans l’URL', () => {
    expect(findOrderView('n-importe-quoi').key).toBe('toutes')
    expect(findOrderView(null).key).toBe('toutes')
  })
})
