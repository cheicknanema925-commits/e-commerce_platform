import type { AgentEvent, AgentProductCard, AgentProposal, ChatLine, ProposalResult } from '../api/agent.ts'

// État d'une conversation avec l'assistant, calculé par un reducer pur : chaque événement du flux SSE devient une
// action, ce qui se teste sans navigateur (comme une machine à états).

export type ProposalState = 'pending' | 'confirming' | 'confirmed' | 'cancelled' | 'failed'

export type ProposalCard = { view: AgentProposal; state: ProposalState; result?: ProposalResult; error?: string }

export type UserEntry = { id: number; role: 'user'; text: string }

export type AssistantEntry = {
  id: number
  role: 'assistant'
  text: string
  streaming: boolean
  products: AgentProductCard[]
  proposal: ProposalCard | null
  warnings: string[]
  error: string | null
}

export type ChatEntry = UserEntry | AssistantEntry

export type ChatState = { entries: ChatEntry[]; nextId: number; busy: boolean }

export type ChatAction =
  | { type: 'history'; lines: ChatLine[] }
  | { type: 'send'; text: string }
  | { type: 'event'; event: AgentEvent }
  | { type: 'failed'; message: string }
  | { type: 'proposal'; id: string; state: ProposalState; result?: ProposalResult; error?: string }
  | { type: 'reset' }

export const initialChatState: ChatState = { entries: [], nextId: 1, busy: false }

function assistant(id: number, text = '', streaming = false): AssistantEntry {
  return { id, role: 'assistant', text, streaming, products: [], proposal: null, warnings: [], error: null }
}

// Applique fn à la réponse en cours (la dernière entrée de l'assistant)
function updateLast(state: ChatState, fn: (entry: AssistantEntry) => AssistantEntry): ChatState {
  const index = state.entries.findLastIndex((entry) => entry.role === 'assistant')
  if (index === -1) {
    return state
  }
  const entries = [...state.entries]
  entries[index] = fn(entries[index] as AssistantEntry)
  return { ...state, entries }
}

export function chatReducer(state: ChatState, action: ChatAction): ChatState {
  switch (action.type) {
    case 'history': {
      // Historique relu au chargement : du texte seul (les cartes d'action passées ne sont plus cliquables)
      const entries = action.lines.map((line, i): ChatEntry =>
        line.role === 'user' ? { id: i + 1, role: 'user', text: line.text } : assistant(i + 1, line.text),
      )
      return { entries, nextId: entries.length + 1, busy: false }
    }
    case 'send':
      return {
        entries: [...state.entries, { id: state.nextId, role: 'user', text: action.text }, assistant(state.nextId + 1, '', true)],
        nextId: state.nextId + 2,
        busy: true,
      }
    case 'event':
      return applyEvent(state, action.event)
    case 'failed':
      return { ...updateLast(state, (entry) => ({ ...entry, streaming: false, error: action.message })), busy: false }
    case 'proposal':
      return {
        ...state,
        entries: state.entries.map((entry) =>
          entry.role === 'assistant' && entry.proposal?.view.id === action.id
            ? { ...entry, proposal: { ...entry.proposal, state: action.state, result: action.result, error: action.error } }
            : entry,
        ),
      }
    case 'reset':
      return initialChatState
  }
}

function applyEvent(state: ChatState, event: AgentEvent): ChatState {
  switch (event.type) {
    case 'token':
      return updateLast(state, (entry) => ({ ...entry, text: entry.text + event.text }))
    case 'products':
      return updateLast(state, (entry) => ({ ...entry, products: event.products }))
    case 'proposal':
      return updateLast(state, (entry) => ({ ...entry, proposal: { view: event.proposal, state: 'pending' } }))
    case 'warning':
      return updateLast(state, (entry) => ({ ...entry, warnings: [...entry.warnings, event.message] }))
    case 'error':
      return updateLast(state, (entry) => ({ ...entry, error: event.message }))
    case 'done':
      return { ...updateLast(state, (entry) => ({ ...entry, streaming: false })), busy: false }
  }
}

// Dernière réponse TERMINÉE : c'est elle qu'on annonce aux lecteurs d'écran (pas chaque mot pendant l'écriture)
export function lastCompletedAnswer(state: ChatState): string {
  const last = state.entries.findLast((entry) => entry.role === 'assistant')
  if (!last || last.role !== 'assistant' || last.streaming) {
    return ''
  }
  return last.error ?? last.text
}
