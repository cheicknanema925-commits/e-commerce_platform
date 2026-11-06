import { type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { AdminTicket, TicketStatus } from '../../api/types.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { inputClass } from '../../components/FormField.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { btnMini, btnMiniDanger, filterInput, PageHeader, Panel } from '../../components/admin/ui.tsx'
import { useAdminTickets, useCloseTicket, usePreOrderDemand, useReplyToTicket } from '../../hooks/useSupport.ts'
import { formatDate } from '../../lib/format.ts'
import { ticketCategoryLabel, ticketStatusLabel } from '../../lib/labels.ts'
import { useToast } from '../../lib/toast.ts'

const STATUSES: TicketStatus[] = ['OPEN', 'ANSWERED', 'CLOSED']

// Demandes des clients (ouvertes sur le site ou préparées par l'assistant) et produits en rupture les plus attendus
export function AdminSupportPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const status = (searchParams.get('statut') as TicketStatus | null) ?? 'OPEN'
  const page = Number(searchParams.get('page') ?? 0)
  const tickets = useAdminTickets(status, page)
  const demand = usePreOrderDemand()

  return (
    <>
      <PageHeader title="Demandes clients" description="Répondez aux clients : ils sont prévenus par e-mail et retrouvent la réponse dans « Mes demandes »." />
      <div className="grid items-start gap-4 xl:grid-cols-[1.6fr_1fr]">
        <div>
          <label htmlFor="statut-demandes" className="sr-only">
            Statut des demandes
          </label>
          <select id="statut-demandes" value={status} onChange={(event) => setSearchParams({ statut: event.target.value })} className={filterInput}>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {ticketStatusLabel(s)}
              </option>
            ))}
          </select>
          {tickets.isError && <ErrorMessage error={tickets.error} />}
          {tickets.data?.content.length === 0 && <p className="mt-6 text-stone-600">Aucune demande avec ce statut.</p>}
          <ul className={`mt-4 space-y-3 transition-opacity ${tickets.isPlaceholderData ? 'opacity-60' : ''}`}>
            {tickets.data?.content.map((item) => (
              <li key={item.ticket.id}>
                <TicketCard item={item} />
              </li>
            ))}
          </ul>
          {tickets.data && (
            <Pagination
              page={tickets.data.page}
              totalPages={tickets.data.totalPages}
              onChange={(next) => setSearchParams({ statut: status, page: String(next) })}
            />
          )}
        </div>

        <Panel title="Produits en rupture les plus attendus">
          {demand.isError && <ErrorMessage error={demand.error} />}
          {demand.data?.length === 0 && <p className="text-sm text-stone-600">Aucune pré-commande en attente.</p>}
          <ul className="space-y-2 text-sm">
            {demand.data?.map((d) => (
              <li key={d.productId} className="flex items-center justify-between gap-3">
                <Link to={`/admin/produits/${d.productId}`} className="font-semibold hover:underline">
                  {d.productName}
                </Link>
                <span className="text-stone-600">
                  {d.requests} client{d.requests > 1 ? 's' : ''} · {d.quantity} article{d.quantity > 1 ? 's' : ''}
                </span>
              </li>
            ))}
          </ul>
          <p className="mt-3 text-xs text-stone-600">Remettre du stock prévient automatiquement ces clients.</p>
        </Panel>
      </div>
    </>
  )
}

function TicketCard({ item }: { item: AdminTicket }) {
  const { ticket } = item
  const reply = useReplyToTicket()
  const close = useCloseTicket()
  const toast = useToast()

  function onReply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const text = String(new FormData(event.currentTarget).get('reply') ?? '').trim()
    if (text) {
      reply.mutate({ id: ticket.id, reply: text }, { onSuccess: () => toast(`Réponse envoyée à ${item.customerName}`) })
    }
  }

  return (
    <article aria-labelledby={`demande-${ticket.id}`} className="rounded-lg border border-stone-200 bg-white p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h2 id={`demande-${ticket.id}`} className="font-semibold">
            {ticket.subject}
          </h2>
          <p className="text-xs text-stone-600">
            {ticketCategoryLabel(ticket.category)} · {item.customerName} ({item.customerEmail}) · {formatDate(ticket.createdAt)}
            {ticket.orderId !== null && (
              <>
                {' · '}
                <Link to={`/admin/commandes/${ticket.orderId}`} className="underline underline-offset-2">
                  commande n° {ticket.orderId}
                </Link>
              </>
            )}
          </p>
        </div>
        {ticket.status !== 'CLOSED' && (
          <button type="button" disabled={close.isPending} onClick={() => close.mutate(ticket.id)} className={btnMiniDanger}>
            Clore<span className="sr-only"> la demande {ticket.subject}</span>
          </button>
        )}
      </div>
      {/* Texte écrit par un client : affiché comme texte, jamais interprété */}
      <p className="mt-2 text-sm whitespace-pre-line">{ticket.message}</p>
      {ticket.reply && (
        <div className="mt-3 rounded-lg bg-brand-50 p-3 text-sm">
          <p className="font-semibold text-brand-700">Votre réponse{ticket.repliedAt ? ` du ${formatDate(ticket.repliedAt)}` : ''}</p>
          <p className="mt-1 whitespace-pre-line">{ticket.reply}</p>
        </div>
      )}
      {ticket.status !== 'CLOSED' && (
        <form onSubmit={onReply} className="mt-3 flex flex-col gap-2">
          <label htmlFor={`reponse-${ticket.id}`} className="text-sm font-semibold">
            {ticket.reply ? 'Compléter la réponse' : 'Répondre'}
          </label>
          <textarea id={`reponse-${ticket.id}`} name="reply" required rows={3} maxLength={2000} className={`${inputClass} resize-y border-stone-200`} />
          <div aria-live="polite">{reply.isError && <ErrorMessage error={reply.error} />}</div>
          <button type="submit" disabled={reply.isPending} className={`${btnMini} self-start`}>
            {reply.isPending ? 'Envoi…' : 'Envoyer la réponse'}
          </button>
        </form>
      )}
    </article>
  )
}
