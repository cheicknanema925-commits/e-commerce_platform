import { type FormEvent, type KeyboardEvent, type ReactNode, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import type { AgentKind, AgentProductCard } from '../../api/agent.ts'
import { useAgentChat } from '../../hooks/useAgentChat.ts'
import { type AssistantEntry, type ProposalCard, lastCompletedAnswer } from '../../lib/agentChat.ts'
import { formatPrice } from '../../lib/format.ts'
import { stockText } from '../../lib/stock.ts'
import { Icon } from '../Icon.tsx'

type AgentChatProps = {
  kind: AgentKind
  intro: ReactNode
  suggestions: string[]
  // Fermer la fenêtre quand on suit un lien vers une fiche produit (boutique)
  onNavigate?: () => void
  autoFocus?: boolean
}

// Conversation avec un assistant. Le texte du modèle est affiché comme TEXTE (jamais interprété en HTML) ; les
// fiches produits et les cartes d'action viennent du serveur, ce sont elles qui font foi.
export function AgentChat({ kind, intro, suggestions, onNavigate, autoFocus = false }: AgentChatProps) {
  const { state, send, confirm, cancel, reset, loading } = useAgentChat(kind)
  const [draft, setDraft] = useState('')
  const input = useRef<HTMLTextAreaElement>(null)
  const bottom = useRef<HTMLDivElement>(null)
  const lastText = state.entries.at(-1)?.role === 'assistant' ? (state.entries.at(-1) as AssistantEntry).text : ''

  useEffect(() => {
    if (autoFocus) {
      input.current?.focus()
    }
  }, [autoFocus])

  // Suit la réponse pendant qu'elle s'écrit
  useEffect(() => {
    bottom.current?.scrollIntoView({ block: 'end' })
  }, [state.entries.length, lastText])

  function submit(text: string) {
    if (!state.busy && text.trim()) {
      void send(text)
      setDraft('')
    }
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    submit(draft)
  }

  // Entrée envoie, Maj+Entrée va à la ligne
  function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault()
      submit(draft)
    }
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="flex-1 space-y-4 overflow-y-auto px-4 py-4">
        {loading && <p className="text-sm text-stone-600">Chargement de la conversation…</p>}
        {!loading && state.entries.length === 0 && (
          <div className="text-sm text-stone-600">
            {intro}
            <ul className="mt-3 flex flex-wrap gap-2" aria-label="Exemples de questions">
              {suggestions.map((suggestion) => (
                <li key={suggestion}>
                  <button
                    type="button"
                    onClick={() => submit(suggestion)}
                    className="rounded-full border border-stone-200 bg-white px-3 py-1.5 text-left text-[13px] font-medium text-stone-900 hover:border-brand-700 hover:text-brand-700"
                  >
                    {suggestion}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

        {state.entries.map((entry) =>
          entry.role === 'user' ? (
            <p key={entry.id} className="ml-auto w-fit max-w-[85%] rounded-2xl rounded-br-md bg-brand-700 px-3.5 py-2.5 text-[15px] break-words whitespace-pre-line text-white">
              <span className="sr-only">Vous : </span>
              {entry.text}
            </p>
          ) : (
            <AssistantMessage key={entry.id} entry={entry} kind={kind} onConfirm={confirm} onCancel={cancel} onNavigate={onNavigate} />
          ),
        )}
        <div ref={bottom} />
      </div>

      {/* Annonce la réponse une fois terminée (et pas chaque mot pendant qu'elle s'écrit) */}
      <p className="sr-only" aria-live="polite">
        {lastCompletedAnswer(state)}
      </p>

      <form onSubmit={onSubmit} className="border-t border-stone-200 bg-white p-3">
        <div className="flex items-end gap-2">
          <label htmlFor={`message-${kind}`} className="sr-only">
            Votre message à l’assistant
          </label>
          <textarea
            id={`message-${kind}`}
            ref={input}
            rows={2}
            maxLength={1000}
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={onKeyDown}
            placeholder="Écrivez votre message…"
            className="max-h-40 min-h-11 flex-1 resize-y rounded-xl border border-stone-200 bg-stone-50 px-3 py-2.5 text-[15px] outline-none focus:border-brand-700 focus:bg-white"
          />
          <button
            type="submit"
            disabled={state.busy || !draft.trim()}
            className="grid size-11 place-items-center rounded-full bg-safran text-stone-900 hover:bg-safran-light disabled:opacity-50"
          >
            <Icon name="send" className="size-5" />
            <span className="sr-only">{state.busy ? 'Réponse en cours' : 'Envoyer'}</span>
          </button>
        </div>
        <div className="mt-2 flex items-center justify-between gap-3 text-xs text-stone-600">
          <span>L’assistant peut se tromper : les fiches et le bouton « Confirmer » font foi.</span>
          {state.entries.length > 0 && (
            <button type="button" onClick={() => void reset()} disabled={state.busy} className="shrink-0 font-medium underline underline-offset-2 hover:text-brand-700 disabled:opacity-50">
              Nouvelle conversation
            </button>
          )}
        </div>
      </form>
    </div>
  )
}

function AssistantMessage({
  entry,
  kind,
  onConfirm,
  onCancel,
  onNavigate,
}: {
  entry: AssistantEntry
  kind: AgentKind
  onConfirm: (id: string) => void
  onCancel: (id: string) => void
  onNavigate?: () => void
}) {
  return (
    <div className="max-w-[92%] space-y-2.5">
      {(entry.text || entry.streaming) && (
        <p className="w-fit rounded-2xl rounded-bl-md bg-stone-100 px-3.5 py-2.5 text-[15px] break-words whitespace-pre-line">
          <span className="sr-only">Assistant : </span>
          {entry.text}
          {entry.streaming && (
            <span aria-hidden="true" className="ml-0.5 inline-block animate-pulse text-stone-400">
              ●
            </span>
          )}
        </p>
      )}

      {entry.products.length > 0 && (
        <ul aria-label="Produits cités" className="grid gap-2 sm:grid-cols-2">
          {entry.products.map((product) => (
            <li key={product.id}>
              <ProductCardLink product={product} kind={kind} onNavigate={onNavigate} />
            </li>
          ))}
        </ul>
      )}

      {entry.proposal && <ProposalBox card={entry.proposal} kind={kind} onConfirm={onConfirm} onCancel={onCancel} />}

      {entry.warnings.map((warning) => (
        <p key={warning} role="status" className="flex gap-2 rounded-lg border border-safran/50 bg-safran-soft px-3 py-2 text-[13px] text-safran-ink">
          <Icon name="bell" className="mt-0.5 size-4" />
          {warning}
        </p>
      ))}

      {entry.error && (
        <p role="alert" className="rounded-lg border border-bissap/30 bg-bissap-soft px-3 py-2 text-[13px] text-bissap">
          {entry.error}
        </p>
      )}
    </div>
  )
}

// Fiche lue en base par le serveur : nom, prix et stock exacts, quoi qu'ait écrit le modèle
function ProductCardLink({ product, kind, onNavigate }: { product: AgentProductCard; kind: AgentKind; onNavigate?: () => void }) {
  const stock = stockText(product.stock)
  return (
    <Link
      to={kind === 'admin' ? `/admin/produits/${product.id}` : `/produits/${product.id}`}
      onClick={onNavigate}
      className="block rounded-xl border border-stone-200 bg-white p-3 text-sm hover:border-brand-700"
    >
      <span className="block font-semibold">{product.nom}</span>
      <span className="block text-xs text-stone-600">
        {[product.marque, product.categorie, product.reference].filter(Boolean).join(' · ')}
      </span>
      <span className="mt-1.5 flex flex-wrap items-baseline gap-x-2">
        <b className="font-bold">{formatPrice(product.prixFcfa)}</b>
        {product.prixAvantRemiseFcfa !== undefined && (
          <s className="text-xs text-stone-500">
            <span className="sr-only">au lieu de </span>
            {formatPrice(product.prixAvantRemiseFcfa)}
          </s>
        )}
        <span className={`text-xs font-medium ${stock.className}`}>{kind === 'admin' ? `${product.stock} en stock` : stock.text}</span>
      </span>
    </Link>
  )
}

function ProposalBox({ card, kind, onConfirm, onCancel }: { card: ProposalCard; kind: AgentKind; onConfirm: (id: string) => void; onCancel: (id: string) => void }) {
  const expires = new Date(card.view.expiresAt).toLocaleTimeString('fr-SN', { hour: '2-digit', minute: '2-digit' })
  const titleId = `proposition-${card.view.id}`
  return (
    <section aria-labelledby={titleId} className="rounded-xl border-2 border-safran bg-white p-3.5 text-sm">
      <h3 id={titleId} className="font-bold">
        {card.state === 'confirmed' ? 'Compte rendu' : kind === 'admin' ? 'Changement à confirmer' : 'Action à confirmer'}
      </h3>

      {card.result ? (
        <ul className="mt-2 space-y-1.5">
          {card.result.steps.map((step) => (
            <li key={step.summary} className={`flex gap-2 ${step.success ? 'text-kola' : 'text-bissap'}`}>
              <Icon name={step.success ? 'check' : 'close'} className="mt-0.5 size-4" />
              <span>
                <span className="text-stone-900">{step.summary}</span> — {step.message}
              </span>
            </li>
          ))}
        </ul>
      ) : (
        <ul className="mt-2 list-disc space-y-1 pl-5">
          {card.view.steps.map((step) => (
            <li key={step}>{step}</li>
          ))}
        </ul>
      )}

      {(card.state === 'pending' || card.state === 'confirming') && (
        <>
          <p className="mt-2 text-xs text-stone-600">Rien n’est fait tant que vous n’avez pas confirmé (avant {expires}).</p>
          <div className="mt-3 flex gap-2">
            <button
              type="button"
              disabled={card.state === 'confirming'}
              onClick={() => onConfirm(card.view.id)}
              className="rounded-full bg-safran px-4 py-2 font-bold text-stone-900 hover:bg-safran-light disabled:opacity-60"
            >
              {card.state === 'confirming' ? 'Confirmation…' : 'Confirmer'}
            </button>
            <button
              type="button"
              disabled={card.state === 'confirming'}
              onClick={() => onCancel(card.view.id)}
              className="rounded-full border border-stone-200 px-4 py-2 font-medium hover:border-bissap hover:text-bissap disabled:opacity-60"
            >
              Annuler
            </button>
          </div>
        </>
      )}
      <div aria-live="polite">
        {card.state === 'cancelled' && <p className="mt-2 font-medium text-stone-600">Annulé : rien n’a été fait.</p>}
        {card.state === 'failed' && (
          <p role="alert" className="mt-2 font-medium text-bissap">
            {card.error}
          </p>
        )}
      </div>
    </section>
  )
}
