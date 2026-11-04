import { describe, expect, it } from 'vitest'
import { formatAxisAmount, formatPeriod, niceScale } from './chart.ts'

describe('niceScale', () => {
  it('arrondit le haut de l’axe à une graduation lisible', () => {
    expect(niceScale(50_000)).toEqual({ top: 60_000, step: 20_000 })
    expect(niceScale(8_000)).toEqual({ top: 8_000, step: 2_000 })
    expect(niceScale(1_234_567)).toEqual({ top: 1_500_000, step: 500_000 })
  })

  it('garde un axe visible quand il n’y a aucune vente', () => {
    expect(niceScale(0)).toEqual({ top: 10_000, step: 2_500 })
  })
})

describe('formatAxisAmount', () => {
  it('écrit les montants en entier sous 10 000 et en compact au-delà', () => {
    expect(formatAxisAmount(2000, 8000)).toMatch(/^2\s000$/u)
    expect(formatAxisAmount(20_000, 60_000)).toMatch(/^20\sk$/u)
  })
})

describe('formatPeriod', () => {
  it('affiche le jour sans décalage de fuseau horaire', () => {
    expect(formatPeriod('2026-11-03', 'DAY', 'short')).toBe('3 nov.')
    expect(formatPeriod('2026-11-03', 'DAY', 'long')).toBe('mardi 3 novembre 2026')
  })

  it('affiche le mois', () => {
    expect(formatPeriod('2026-11-01', 'MONTH', 'long')).toBe('novembre 2026')
  })
})
