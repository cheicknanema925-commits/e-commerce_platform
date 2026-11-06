import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useReducer, useRef } from 'react'
import { type AgentKind, cancelProposal, clearConversation, confirmProposal, fetchConversation, streamAgentChat } from '../api/agent.ts'
import { chatReducer, initialChatState } from '../lib/agentChat.ts'

// Données qu'une action confirmée a pu changer : on les relit pour que le reste de la page soit à jour
const AFFECTED: Record<AgentKind, string[][]> = {
  client: [['cart'], ['preorders'], ['support']],
  admin: [['products'], ['product'], ['admin'], ['price-changes']],
}

export function useAgentChat(kind: AgentKind) {
  const [state, dispatch] = useReducer(chatReducer, initialChatState)
  const queryClient = useQueryClient()
  const stream = useRef<AbortController | null>(null)
  const history = useQuery({ queryKey: ['agent', kind, 'history'], queryFn: () => fetchConversation(kind), staleTime: Infinity })
  const historyLoaded = useRef(false)

  useEffect(() => {
    if (history.data && !historyLoaded.current) {
      historyLoaded.current = true
      dispatch({ type: 'history', lines: history.data })
    }
  }, [history.data])

  // Composant retiré (page quittée) : on coupe le flux en cours
  useEffect(() => () => stream.current?.abort(), [])

  async function send(text: string) {
    const message = text.trim()
    if (!message || state.busy) {
      return
    }
    dispatch({ type: 'send', text: message })
    const controller = new AbortController()
    stream.current = controller
    try {
      await streamAgentChat(kind, message, (event) => dispatch({ type: 'event', event }), controller.signal)
      // Flux coupé sans « done » (serveur redémarré…) : on libère quand même la saisie
      dispatch({ type: 'event', event: { type: 'done' } })
    } catch (error) {
      if (!controller.signal.aborted) {
        dispatch({ type: 'failed', message: error instanceof Error ? error.message : 'L’assistant est injoignable.' })
      }
    }
  }

  async function confirm(id: string) {
    dispatch({ type: 'proposal', id, state: 'confirming' })
    try {
      const result = await confirmProposal(kind, id)
      dispatch({ type: 'proposal', id, state: 'confirmed', result })
      AFFECTED[kind].forEach((queryKey) => queryClient.invalidateQueries({ queryKey }))
    } catch (error) {
      dispatch({ type: 'proposal', id, state: 'failed', error: error instanceof Error ? error.message : 'Confirmation impossible.' })
    }
  }

  async function cancel(id: string) {
    try {
      await cancelProposal(kind, id)
      dispatch({ type: 'proposal', id, state: 'cancelled' })
    } catch (error) {
      dispatch({ type: 'proposal', id, state: 'failed', error: error instanceof Error ? error.message : 'Annulation impossible.' })
    }
  }

  async function reset() {
    stream.current?.abort()
    await clearConversation(kind)
    queryClient.setQueryData(['agent', kind, 'history'], [])
    dispatch({ type: 'reset' })
  }

  return { state, send, confirm, cancel, reset, loading: history.isPending }
}
