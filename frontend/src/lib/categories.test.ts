import { describe, expect, it } from 'vitest'
import { categoryVisual } from './categories.ts'

describe('categoryVisual', () => {
  it('donne le visuel prévu pour un rayon connu', () => {
    expect(categoryVisual('electronique')).toEqual({ emoji: '💻', tint: 'bg-tint-sky' })
  })

  it('donne un visuel par défaut pour un rayon ajouté plus tard', () => {
    expect(categoryVisual('jardinage').emoji).toBe('📦')
    expect(categoryVisual(undefined).emoji).toBe('📦')
  })
})
