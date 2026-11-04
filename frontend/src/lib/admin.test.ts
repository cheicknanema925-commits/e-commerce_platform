import { describe, expect, it } from 'vitest'
import { cashState, initials, percent } from './admin.ts'

describe('initials', () => {
  it('garde les deux premières initiales', () => {
    expect(initials('Mariama Sow')).toBe('MS')
    expect(initials('  awa   diop fall ')).toBe('AD')
    expect(initials('Administrateur')).toBe('A')
  })
})

describe('percent', () => {
  it('arrondit et évite la division par zéro', () => {
    expect(percent(1, 3)).toBe('33 %')
    expect(percent(0, 0)).toBe('0 %')
  })
})

describe('cashState', () => {
  it('suit les espèces : à encaisser tant que la commande n’est pas livrée', () => {
    expect(cashState({ status: 'SHIPPED', paymentMethod: 'CASH_ON_DELIVERY' })).toBe('due')
    expect(cashState({ status: 'DELIVERED', paymentMethod: 'CASH_ON_DELIVERY' })).toBe('collected')
    expect(cashState({ status: 'CANCELLED', paymentMethod: 'CASH_ON_DELIVERY' })).toBe('cancelled')
  })

  it('ne concerne pas les paiements en ligne', () => {
    expect(cashState({ status: 'PAID', paymentMethod: 'ONLINE' })).toBeNull()
  })
})
