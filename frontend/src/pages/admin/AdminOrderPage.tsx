import { Link, useParams } from 'react-router'
import { ApiError } from '../../api/client.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { useAdminDelivery, useAdminOrder, useChangeOrderStatus } from '../../hooks/useAdmin.ts'
import { formatDate, formatPrice } from '../../lib/format.ts'
import { adminActionLabel, orderStatusLabel, PAYMENT_METHOD_LABELS } from '../../lib/labels.ts'
import { NotFoundPage } from '../NotFoundPage.tsx'

export function AdminOrderPage() {
  const id = Number(useParams().id)
  const order = useAdminOrder(id)
  const delivery = useAdminDelivery(id)
  const changeStatus = useChangeOrderStatus()

  if (order.error instanceof ApiError && order.error.status === 404) {
    return <NotFoundPage />
  }
  if (order.isError) {
    return <ErrorMessage error={order.error} />
  }
  if (!order.data) {
    return <p className="text-stone-600">Chargement…</p>
  }

  const { status, paymentMethod, totalAmount, createdAt, customerName, customerEmail, items, nextStatuses } = order.data
  return (
    <>
      <Link to="/admin/commandes" className="text-sm font-medium text-brand-700 hover:underline">
        ← Commandes
      </Link>
      <div className="mt-2 flex flex-wrap items-baseline justify-between gap-4">
        <h2 className="text-xl font-semibold">Commande n° {id}</h2>
        <p className="rounded-full bg-stone-200 px-3 py-1 text-sm font-medium">{orderStatusLabel(status, paymentMethod)}</p>
      </div>
      <p className="text-sm text-stone-600">
        {formatDate(createdAt)} · {customerName} ({customerEmail}) · {PAYMENT_METHOD_LABELS[paymentMethod]}
      </p>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <section aria-labelledby="articles" className="rounded-lg border border-stone-200 bg-white p-5 lg:col-span-2">
          <h3 id="articles" className="font-semibold">
            Articles
          </h3>
          <ul className="mt-3 divide-y divide-stone-200 text-sm">
            {items.map((item) => (
              <li key={item.productId} className="flex justify-between gap-4 py-2">
                <span>
                  {item.quantity} × {item.productName}
                </span>
                <span>{formatPrice(item.subtotal)}</span>
              </li>
            ))}
          </ul>
          <p className="mt-3 flex justify-between border-t border-stone-200 pt-3 font-semibold">
            <span>Total</span>
            <span>{formatPrice(totalAmount)}</span>
          </p>

          {delivery.data && (
            <div className="mt-6 text-sm">
              <h3 className="font-semibold">Livraison</h3>
              <p className="mt-2">
                {delivery.data.addressLine}, {delivery.data.city} · {delivery.data.phone}
              </p>
              {delivery.data.latitude !== null && delivery.data.longitude !== null && (
                <a
                  href={`https://www.openstreetmap.org/?mlat=${delivery.data.latitude}&mlon=${delivery.data.longitude}#map=17/${delivery.data.latitude}/${delivery.data.longitude}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-1 inline-block font-medium text-brand-700 hover:underline"
                >
                  Voir le point de livraison sur la carte<span className="sr-only"> (nouvel onglet)</span>
                </a>
              )}
            </div>
          )}
        </section>

        <aside className="flex h-fit flex-col gap-3 rounded-lg border border-stone-200 bg-white p-5">
          <h3 className="font-semibold">Actions</h3>
          {nextStatuses.length === 0 && <p className="text-sm text-stone-600">Aucune action possible : commande terminée ou en attente de paiement.</p>}
          {nextStatuses.map((target) => (
            <button
              key={target}
              type="button"
              disabled={changeStatus.isPending}
              onClick={() => {
                if (target !== 'CANCELLED' || window.confirm('Annuler cette commande ? Les articles seront remis en stock.')) {
                  changeStatus.mutate({ id, status: target })
                }
              }}
              className={
                target === 'CANCELLED'
                  ? 'rounded-md border border-red-300 px-4 py-2 font-medium text-red-700 hover:bg-red-50 disabled:opacity-60'
                  : 'rounded-md bg-brand-700 px-4 py-2 font-medium text-white hover:bg-brand-800 disabled:opacity-60'
              }
            >
              {adminActionLabel(target, paymentMethod)}
            </button>
          ))}
          <div aria-live="polite">{changeStatus.isError && <ErrorMessage error={changeStatus.error} />}</div>
        </aside>
      </div>
    </>
  )
}
