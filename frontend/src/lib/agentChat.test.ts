import { describe, expect, it } from 'vitest'
import type { AgentEvent } from '../api/agent.ts'
import { type AssistantEntry, type ChatState, chatReducer, initialChatState, lastCompletedAnswer } from './agentChat.ts'

const PROPOSAL = { id: 'p1', steps: ['Ajouter 2 × Sandales cuir au panier (8000 FCFA l’unité)'], expiresAt: '2026-11-05T10:15:00Z' }

function play(...events: AgentEvent[]): ChatState {
  return events.reduce((state, event) => chatReducer(state, { type: 'event', event }), chatReducer(initialChatState, { type: 'send', text: 'Ajoute 2 sandales' }))
}

function last(state: ChatState): AssistantEntry {
  return state.entries.at(-1) as AssistantEntry
}

describe('chatReducer', () => {
  it('ajoute la question puis une réponse vide « en cours d’écriture »', () => {
    const state = chatReducer(initialChatState, { type: 'send', text: 'Bonjour' })

    expect(state.busy).toBe(true)
    expect(state.entries).toMatchObject([
      { role: 'user', text: 'Bonjour' },
      { role: 'assistant', text: '', streaming: true },
    ])
  })

  it('assemble le texte reçu morceau par morceau, puis les fiches, la carte d’action et les avertissements', () => {
    const state = play(
      { type: 'token', text: 'J’ai ' },
      { type: 'token', text: 'préparé l’ajout.' },
      { type: 'products', products: [{ id: 2, nom: 'Sandales cuir', categorie: 'Mode', prixFcfa: 8000, stock: 9 }] },
      { type: 'proposal', proposal: PROPOSAL },
      { type: 'warning', message: 'Montant à vérifier' },
      { type: 'done' },
    )

    expect(state.busy).toBe(false)
    expect(last(state)).toMatchObject({
      text: 'J’ai préparé l’ajout.',
      streaming: false,
      products: [{ nom: 'Sandales cuir' }],
      proposal: { view: PROPOSAL, state: 'pending' },
      warnings: ['Montant à vérifier'],
    })
  })

  it('suit la décision sur la carte : confirmation, puis compte rendu de chaque étape', () => {
    let state = play({ type: 'proposal', proposal: PROPOSAL }, { type: 'done' })
    state = chatReducer(state, { type: 'proposal', id: 'p1', state: 'confirming' })
    expect(last(state).proposal?.state).toBe('confirming')

    const result = { steps: [{ summary: PROPOSAL.steps[0], success: true, message: 'ajouté au panier' }] }
    state = chatReducer(state, { type: 'proposal', id: 'p1', state: 'confirmed', result })

    expect(last(state).proposal).toMatchObject({ state: 'confirmed', result })
  })

  it('affiche une erreur réseau ou un refus de l’API (429) et libère la saisie', () => {
    const state = chatReducer(chatReducer(initialChatState, { type: 'send', text: 'Bonjour' }), {
      type: 'failed',
      message: 'Trop de messages : patientez une minute.',
    })

    expect(state.busy).toBe(false)
    expect(last(state)).toMatchObject({ streaming: false, error: 'Trop de messages : patientez une minute.' })
  })

  it('recharge l’historique en texte seul et repart des bons identifiants', () => {
    const state = chatReducer(initialChatState, {
      type: 'history',
      lines: [
        { role: 'user', text: 'Bonjour' },
        { role: 'assistant', text: 'Bonjour, que cherchez-vous ?' },
      ],
    })

    expect(state.entries.map((e) => e.id)).toEqual([1, 2])
    expect(chatReducer(state, { type: 'send', text: 'Des sandales' }).entries.at(-1)?.id).toBe(4)
  })

  it('n’annonce aux lecteurs d’écran que la réponse terminée', () => {
    const writing = play({ type: 'token', text: 'Je cherche' })
    expect(lastCompletedAnswer(writing)).toBe('')
    expect(lastCompletedAnswer(chatReducer(writing, { type: 'event', event: { type: 'done' } }))).toBe('Je cherche')
  })
})
