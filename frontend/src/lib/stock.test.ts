import { describe, expect, it } from 'vitest'
import { stockText } from './stock.ts'

describe('stockText', () => {
  it('distingue rupture, presque épuisé et disponible', () => {
    expect(stockText(0).text).toBe('Rupture de stock')
    expect(stockText(3).text).toBe('Plus que 3 en stock')
    expect(stockText(5).text).toBe('Plus que 5 en stock')
    expect(stockText(6).text).toBe('En stock')
  })
})
