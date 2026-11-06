import { apiGet, apiRequest, apiStream } from './client.ts'
import { createSseParser, type SseMessage } from '../lib/sse.ts'

// Deux assistants, deux routes : /api/agent (client connecté) et /api/admin/agent (rôle ADMIN, vérifié par l'API)
export type AgentKind = 'client' | 'admin'

const BASE: Record<AgentKind, string> = { client: '/agent', admin: '/admin/agent' }

// Fiche produit envoyée par le SERVEUR (lue en base) : c'est elle qui fait foi, pas le texte du modèle
export type AgentProductCard = {
  id: number
  nom: string
  marque?: string
  categorie: string
  prixFcfa: number
  prixAvantRemiseFcfa?: number
  stock: number
  reference?: string
}

export type AgentProposal = { id: string; steps: string[]; expiresAt: string }

export type AgentEvent =
  | { type: 'token'; text: string }
  | { type: 'products'; products: AgentProductCard[] }
  | { type: 'proposal'; proposal: AgentProposal }
  | { type: 'warning'; message: string }
  | { type: 'error'; message: string }
  | { type: 'done' }

export type ProposalResult = { steps: { summary: string; success: boolean; message: string }[] }

export type ChatLine = { role: 'user' | 'assistant'; text: string }

// Message SSE → événement typé. Un nom inconnu ou un JSON illisible est ignoré (jamais d'exception au milieu du flux)
export function toAgentEvent(message: SseMessage): AgentEvent | null {
  let data: Record<string, unknown>
  try {
    data = JSON.parse(message.data) as Record<string, unknown>
  } catch {
    return null
  }
  switch (message.event) {
    case 'token':
      return typeof data.text === 'string' ? { type: 'token', text: data.text } : null
    case 'products':
      return Array.isArray(data.products) ? { type: 'products', products: data.products as AgentProductCard[] } : null
    case 'proposal':
      return data.proposal ? { type: 'proposal', proposal: data.proposal as AgentProposal } : null
    case 'warning':
    case 'error':
      return typeof data.message === 'string' ? { type: message.event, message: data.message } : null
    case 'done':
      return { type: 'done' }
    default:
      return null
  }
}

// Envoie le message et appelle onEvent au fur et à mesure que la réponse arrive
export async function streamAgentChat(kind: AgentKind, message: string, onEvent: (event: AgentEvent) => void, signal?: AbortSignal) {
  const body = await apiStream(`${BASE[kind]}/chat`, { message }, signal)
  const parser = createSseParser((sse) => {
    const event = toAgentEvent(sse)
    if (event) {
      onEvent(event)
    }
  })
  const reader = body.getReader()
  // stream: true : un caractère accentué (plusieurs octets) coupé entre deux morceaux est recollé correctement
  const decoder = new TextDecoder()
  for (;;) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    parser.push(decoder.decode(value, { stream: true }))
  }
  parser.push(decoder.decode())
  parser.end()
}

export function fetchConversation(kind: AgentKind): Promise<ChatLine[]> {
  return apiGet(`${BASE[kind]}/conversation`)
}

export function clearConversation(kind: AgentKind): Promise<void> {
  return apiRequest('DELETE', `${BASE[kind]}/conversation`)
}

// La confirmation est un CLIC de l'utilisateur : c'est la seule façon d'exécuter une action proposée par l'assistant
export function confirmProposal(kind: AgentKind, id: string): Promise<ProposalResult> {
  return apiRequest('POST', `${BASE[kind]}/proposals/${id}/confirm`)
}

export function cancelProposal(kind: AgentKind, id: string): Promise<void> {
  return apiRequest('POST', `${BASE[kind]}/proposals/${id}/cancel`)
}
