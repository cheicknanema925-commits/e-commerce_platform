import { describe, expect, it } from 'vitest'
import { formatPrice } from './format.ts'

describe('formatPrice', () => {
  it('affiche des francs CFA sans centimes, avec séparateur de milliers', () => {
    // \s couvre l'espace insécable fine utilisée par le format français
    expect(formatPrice(25000)).toMatch(/^25\s000\sF\sCFA$/u)
  })

  it('arrondit les montants reçus avec des décimales (25000.00 envoyé par l’API)', () => {
    expect(formatPrice(8000.0)).toMatch(/^8\s000\sF\sCFA$/u)
  })
})
