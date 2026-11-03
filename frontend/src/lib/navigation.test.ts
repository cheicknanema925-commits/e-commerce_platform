import { describe, expect, it } from 'vitest'
import { safeReturnPath } from './navigation.ts'

describe('safeReturnPath', () => {
  it('accepte un chemin interne', () => {
    expect(safeReturnPath('/panier')).toBe('/panier')
    expect(safeReturnPath('/commandes/12?paiement=retour')).toBe('/commandes/12?paiement=retour')
  })

  it.each(['https://site-pirate.example', '//site-pirate.example', '/\\site-pirate.example', 'javascript:alert(1)', ''])(
    'refuse « %s » (redirection ouverte) et renvoie vers l’accueil',
    (value) => {
      expect(safeReturnPath(value)).toBe('/')
    },
  )
})
