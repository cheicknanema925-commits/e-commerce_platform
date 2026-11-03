import { describe, expect, it } from 'vitest'
import { adminActionLabel, orderStatusLabel } from './labels.ts'

describe('orderStatusLabel', () => {
  it('distingue l’attente de paiement (en ligne) de l’attente de confirmation (espèces)', () => {
    expect(orderStatusLabel('CREATED', 'ONLINE')).toBe('En attente de paiement')
    expect(orderStatusLabel('CREATED', 'CASH_ON_DELIVERY')).toBe('En attente de confirmation')
  })
})

describe('adminActionLabel', () => {
  it('rappelle à l’admin d’encaisser les espèces quand il marque une commande livrée', () => {
    expect(adminActionLabel('DELIVERED', 'CASH_ON_DELIVERY')).toBe('Livrée, espèces encaissées')
    expect(adminActionLabel('DELIVERED', 'ONLINE')).toBe('Marquer comme livrée')
  })

  it('confirme une commande en espèces au moment de la préparer', () => {
    expect(adminActionLabel('PREPARING', 'CASH_ON_DELIVERY')).toBe('Confirmer et préparer')
    expect(adminActionLabel('PREPARING', 'ONLINE')).toBe('Passer en préparation')
  })
})
