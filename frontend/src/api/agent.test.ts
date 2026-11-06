import { describe, expect, it } from 'vitest'
import { toAgentEvent } from './agent.ts'

describe('toAgentEvent', () => {
  it('convertit chaque événement envoyé par l’API', () => {
    expect(toAgentEvent({ event: 'token', data: '{"text":"Bonjour"}' })).toEqual({ type: 'token', text: 'Bonjour' })
    expect(toAgentEvent({ event: 'products', data: '{"products":[{"id":2,"nom":"Sandales cuir","prixFcfa":8000}]}' })).toMatchObject({
      type: 'products',
      products: [{ id: 2, nom: 'Sandales cuir', prixFcfa: 8000 }],
    })
    expect(toAgentEvent({ event: 'proposal', data: '{"proposal":{"id":"abc","steps":["Ajouter"],"expiresAt":"2026-11-05T10:00:00Z"}}' }))
      .toMatchObject({ type: 'proposal', proposal: { id: 'abc', steps: ['Ajouter'] } })
    expect(toAgentEvent({ event: 'warning', data: '{"message":"Montant à vérifier"}' })).toEqual({ type: 'warning', message: 'Montant à vérifier' })
    expect(toAgentEvent({ event: 'error', data: '{"message":"Indisponible"}' })).toEqual({ type: 'error', message: 'Indisponible' })
    expect(toAgentEvent({ event: 'done', data: '{}' })).toEqual({ type: 'done' })
  })

  it('ignore un événement inconnu, mal formé ou au JSON illisible', () => {
    expect(toAgentEvent({ event: 'inconnu', data: '{}' })).toBeNull()
    expect(toAgentEvent({ event: 'token', data: '{"texte":"x"}' })).toBeNull()
    expect(toAgentEvent({ event: 'token', data: 'pas du json' })).toBeNull()
  })
})
