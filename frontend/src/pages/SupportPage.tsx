import { type FormEvent, useState } from 'react'
import { Link } from 'react-router'
import type { TicketCategory } from '../api/types.ts'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { FormField, inputClass } from '../components/FormField.tsx'
import { useMyOrders } from '../hooks/useOrders.ts'
import { useCancelPreOrder, useMyPreOrders, useMyTickets, useOpenTicket } from '../hooks/useSupport.ts'
import { formatDate } from '../lib/format.ts'
import { fieldErrors } from '../lib/forms.ts'
import { TICKET_CATEGORIES, ticketCategoryLabel, ticketStatusLabel } from '../lib/labels.ts'
import { useToast } from '../lib/toast.ts'

const STATUS_STYLE = { OPEN: 'bg-safran-soft text-safran-ink', ANSWERED: 'bg-kola-soft text-kola', CLOSED: 'bg-stone-100 text-stone-600' }

// Service client : écrire une demande (aussi possible via l'assistant), lire les réponses, suivre les produits attendus
export function SupportPage() {
  const tickets = useMyTickets()
  const orders = useMyOrders()
  const preOrders = useMyPreOrders()
  const openTicket = useOpenTicket()
  const cancelPreOrder = useCancelPreOrder()
  const toast = useToast()
  const [formKey, setFormKey] = useState(0)
  const errors = fieldErrors(openTicket.error)
  const waiting = preOrders.data?.filter((p) => p.status === 'WAITING') ?? []

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const orderId = String(form.get('orderId') ?? '')
    openTicket.mutate(
      {
        category: String(form.get('category')) as TicketCategory,
        subject: String(form.get('subject')).trim(),
        message: String(form.get('message')).trim(),
        orderId: orderId ? Number(orderId) : null,
      },
      {
        onSuccess: () => {
          toast('Demande envoyée au service client')
          // Nouveau formulaire vide
          setFormKey((key) => key + 1)
        },
      },
    )
  }

  return (
    <>
      <h1 className="text-[clamp(28px,3.4vw,40px)] font-bold tracking-tight">Mes demandes</h1>
      <p className="mt-1 text-stone-600">Écrivez au service client et retrouvez ses réponses. L’assistant peut aussi préparer une demande pour vous.</p>

      <div className="mt-6 grid items-start gap-6 lg:grid-cols-[1fr_1.2fr]">
        <form key={formKey} onSubmit={onSubmit} className="space-y-3.5 rounded-[18px] border border-stone-200 bg-white p-5">
          <h2 className="text-lg font-bold">Nouvelle demande</h2>
          {openTicket.isError && Object.keys(errors).length === 0 && <ErrorMessage error={openTicket.error} />}
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="category" className="text-sm font-semibold">
                Sujet
              </label>
              <select id="category" name="category" required defaultValue="DELIVERY" className={`${inputClass} border-stone-200`}>
                {TICKET_CATEGORIES.map((c) => (
                  <option key={c.value} value={c.value}>
                    {c.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1.5">
              <label htmlFor="orderId" className="text-sm font-semibold">
                Commande concernée
              </label>
              <select id="orderId" name="orderId" defaultValue="" className={`${inputClass} border-stone-200`}>
                <option value="">Aucune</option>
                {orders.data?.map((order) => (
                  <option key={order.id} value={order.id}>
                    N° {order.id} du {formatDate(order.createdAt)}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <FormField name="subject" label="Objet *" required maxLength={120} placeholder="Ex. Colis toujours pas reçu" error={errors.subject} />
          <div className="flex flex-col gap-1.5">
            <label htmlFor="message" className="text-sm font-semibold">
              Message *
            </label>
            <textarea id="message" name="message" required rows={5} maxLength={2000} className={`${inputClass} resize-y border-stone-200`} />
            {errors.message && <p className="text-xs text-bissap">{errors.message}</p>}
          </div>
          <button type="submit" disabled={openTicket.isPending} className="rounded-full bg-safran px-6 py-3 font-bold text-stone-900 hover:bg-safran-light disabled:opacity-60">
            {openTicket.isPending ? 'Envoi…' : 'Envoyer la demande'}
          </button>
        </form>

        <section aria-labelledby="titre-demandes">
          <h2 id="titre-demandes" className="text-lg font-bold">
            Demandes envoyées
          </h2>
          {tickets.isError && <ErrorMessage error={tickets.error} />}
          {tickets.data?.length === 0 && <p className="mt-3 text-stone-600">Aucune demande pour le moment.</p>}
          <ul className="mt-3 space-y-3">
            {tickets.data?.map((ticket) => (
              <li key={ticket.id} className="rounded-lg border border-stone-200 bg-white p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h3 className="font-semibold">{ticket.subject}</h3>
                  <span className={`rounded-full px-2.5 py-0.5 text-[13px] font-semibold ${STATUS_STYLE[ticket.status]}`}>{ticketStatusLabel(ticket.status)}</span>
                </div>
                <p className="text-xs text-stone-600">
                  {ticketCategoryLabel(ticket.category)} · {formatDate(ticket.createdAt)}
                  {ticket.orderId !== null && (
                    <>
                      {' · '}
                      <Link to={`/commandes/${ticket.orderId}`} className="underline underline-offset-2">
                        commande n° {ticket.orderId}
                      </Link>
                    </>
                  )}
                </p>
                <p className="mt-2 text-sm whitespace-pre-line">{ticket.message}</p>
                {ticket.reply && (
                  <div className="mt-3 rounded-lg bg-brand-50 p-3 text-sm">
                    <p className="font-semibold text-brand-700">Réponse du service client</p>
                    <p className="mt-1 whitespace-pre-line">{ticket.reply}</p>
                  </div>
                )}
              </li>
            ))}
          </ul>

          <h2 className="mt-8 text-lg font-bold">Produits attendus</h2>
          <p className="text-sm text-stone-600">Vous êtes prévenu par e-mail dès leur retour en stock (rien n’est réservé).</p>
          {waiting.length === 0 && <p className="mt-3 text-stone-600">Aucun produit attendu.</p>}
          <ul className="mt-3 space-y-2">
            {waiting.map((p) => (
              <li key={p.id} className="flex items-center justify-between gap-3 rounded-lg border border-stone-200 bg-white px-4 py-3 text-sm">
                <Link to={`/produits/${p.productId}`} className="font-semibold hover:underline">
                  {p.productName}
                </Link>
                <button
                  type="button"
                  disabled={cancelPreOrder.isPending}
                  onClick={() => cancelPreOrder.mutate(p.id)}
                  className="text-[13px] font-medium text-bissap underline underline-offset-2"
                >
                  Ne plus attendre<span className="sr-only"> {p.productName}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </>
  )
}
