import { Link, useParams, useSearchParams } from 'react-router'
import { ApiError } from '../api/client.ts'
import { DeliveryTracking } from '../components/DeliveryTracking.tsx'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { useCancelOrder, useDelivery, useOrder, useStartPayment } from '../hooks/useOrders.ts'
import { formatDate, formatPrice } from '../lib/format.ts'
import { orderStatusLabel, PAYMENT_METHOD_LABELS } from '../lib/labels.ts'
import { NotFoundPage } from './NotFoundPage.tsx'

export function OrderPage() {
  const id = Number(useParams().id)
  const [searchParams] = useSearchParams()
  const paymentReturn = searchParams.get('paiement')
  // Retour de la page de paiement : on attend la confirmation du SERVEUR (webhook vérifié), pas l'URL de retour
  const order = useOrder(id, paymentReturn === 'retour')
  const delivery = useDelivery(id)
  const cancel = useCancelOrder()
  const startPayment = useStartPayment()

  if (order.error instanceof ApiError && order.error.status === 404) {
    return <NotFoundPage />
  }
  if (order.isError) {
    return <ErrorMessage error={order.error} />
  }
  if (!order.data) {
    return <p className="text-stone-600">Chargement de la commande…</p>
  }

  const { status, paymentMethod, totalAmount, createdAt, items } = order.data
  const waitingForPayment = status === 'CREATED' && paymentMethod === 'ONLINE'

  async function pay() {
    const payment = await startPayment.mutateAsync(id)
    window.location.assign(payment.checkoutUrl)
  }

  return (
    <>
      <Link to="/commandes" className="text-sm font-medium text-brand-700 hover:underline">
        ← Mes commandes
      </Link>
      <div className="mt-4 flex flex-wrap items-baseline justify-between gap-4">
        <h1 className="text-3xl font-bold tracking-tight">Commande n° {id}</h1>
        <p className="rounded-full bg-brand-50 px-3 py-1 text-sm font-medium text-brand-800">{orderStatusLabel(status, paymentMethod)}</p>
      </div>
      <p className="mt-1 text-sm text-stone-600">
        Passée le {formatDate(createdAt)} · {PAYMENT_METHOD_LABELS[paymentMethod]}
      </p>

      <div aria-live="polite" className="mt-4 space-y-2">
        {searchParams.get('confirmation') && (
          <p className="rounded-md bg-brand-50 p-4 text-brand-900">Merci ! Votre commande est enregistrée. Vous paierez en espèces au livreur.</p>
        )}
        {paymentReturn === 'retour' && waitingForPayment && (
          <p className="rounded-md bg-amber-50 p-4 text-amber-900">Paiement en cours de vérification auprès du fournisseur…</p>
        )}
        {paymentReturn === 'retour' && status === 'PAID' && (
          <p className="rounded-md bg-brand-50 p-4 text-brand-900">Paiement confirmé. Merci pour votre commande !</p>
        )}
        {paymentReturn === 'annule' && waitingForPayment && (
          <p className="rounded-md bg-stone-100 p-4 text-stone-800">Paiement annulé. Vous pouvez réessayer quand vous voulez.</p>
        )}
        {(cancel.isError || startPayment.isError) && <ErrorMessage error={cancel.error ?? startPayment.error} />}
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <section aria-labelledby="articles" className="rounded-lg border border-stone-200 bg-white p-6 lg:col-span-2">
          <h2 id="articles" className="text-lg font-semibold">
            Articles
          </h2>
          <ul className="mt-4 divide-y divide-stone-200">
            {items.map((item) => (
              <li key={item.productId} className="flex justify-between gap-4 py-3">
                <span>
                  {item.quantity} × {item.productName}
                  <span className="block text-sm text-stone-600">{formatPrice(item.unitPrice)} l'unité</span>
                </span>
                <span className="font-semibold">{formatPrice(item.subtotal)}</span>
              </li>
            ))}
          </ul>
          <p className="mt-4 flex justify-between border-t border-stone-200 pt-4 text-lg">
            <span>Total</span>
            <span className="font-bold text-brand-800">{formatPrice(totalAmount)}</span>
          </p>
        </section>

        <aside className="flex h-fit flex-col gap-3 rounded-lg border border-stone-200 bg-white p-6">
          {waitingForPayment && (
            <button
              type="button"
              onClick={() => pay().catch(() => undefined)}
              disabled={startPayment.isPending}
              className="rounded-md bg-brand-700 px-4 py-3 font-medium text-white hover:bg-brand-800 disabled:opacity-60"
            >
              Payer {formatPrice(totalAmount)}
            </button>
          )}
          {status === 'CREATED' && (
            <button
              type="button"
              disabled={cancel.isPending}
              onClick={() => {
                if (window.confirm('Annuler cette commande ? Les articles seront remis en stock.')) {
                  cancel.mutate(id)
                }
              }}
              className="rounded-md border border-red-300 px-4 py-2 font-medium text-red-700 hover:bg-red-50"
            >
              Annuler la commande
            </button>
          )}
          {status !== 'CREATED' && <p className="text-sm text-stone-600">Besoin d'aide ? Contactez le service client.</p>}
        </aside>
      </div>

      {delivery.data && (
        <div className="mt-6">
          <DeliveryTracking delivery={delivery.data} />
        </div>
      )}
    </>
  )
}
