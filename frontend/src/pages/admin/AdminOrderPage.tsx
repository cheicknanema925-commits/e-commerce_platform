import { Link, useParams } from 'react-router'
import { ApiError } from '../../api/client.ts'
import { DeliveryTracking } from '../../components/DeliveryTracking.tsx'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { OrderStatusBadge } from '../../components/OrderStatusBadge.tsx'
import { PaymentCell } from '../../components/admin/PaymentCell.tsx'
import { CourierSelect } from '../../components/admin/CourierSelect.tsx'
import { btnGhost, PageHeader, Panel } from '../../components/admin/ui.tsx'
import { useAdminDelivery, useAdminOrder, useChangeOrderStatus } from '../../hooks/useAdmin.ts'
import { formatDate, formatPrice } from '../../lib/format.ts'
import { adminActionLabel } from '../../lib/labels.ts'
import { useToast } from '../../lib/toast.ts'
import { NotFoundPage } from '../NotFoundPage.tsx'

export function AdminOrderPage() {
  const id = Number(useParams().id)
  const order = useAdminOrder(id)
  const delivery = useAdminDelivery(id)
  const changeStatus = useChangeOrderStatus()
  const toast = useToast()

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
      <PageHeader
        title={`Commande n° ${id}`}
        description={
          <span className="inline-flex flex-wrap items-center gap-2">
            Passée le {formatDate(createdAt)} <OrderStatusBadge status={status} paymentMethod={paymentMethod} />
          </span>
        }
        actions={
          <Link to="/admin/commandes" className={btnGhost}>
            Retour aux commandes
          </Link>
        }
      />

      <div className="grid items-start gap-4 xl:grid-cols-[1.55fr_1fr]">
        <div className="flex min-w-0 flex-col gap-4">
          <Panel title="Articles">
            <ul className="divide-y divide-stone-200 text-sm">
              {items.map((item) => (
                <li key={item.productId} className="flex justify-between gap-4 py-2.5">
                  <span>
                    <Link to={`/admin/produits/${item.productId}`} className="font-medium hover:underline">
                      {item.productName}
                    </Link>
                    <span className="block text-stone-600">
                      {item.quantity} × {formatPrice(item.unitPrice)}
                    </span>
                  </span>
                  <span className="font-semibold tabular-nums">{formatPrice(item.subtotal)}</span>
                </li>
              ))}
            </ul>
            {order.data.serviceFee > 0 && (
              <p className="mt-3 flex justify-between border-t border-stone-200 pt-3 text-sm">
                <span>Frais de service (espèces)</span>
                <span className="tabular-nums">{formatPrice(order.data.serviceFee)}</span>
              </p>
            )}
            <p className="mt-3 flex justify-between border-t border-stone-200 pt-3 text-lg font-bold">
              <span>Total</span>
              <span>{formatPrice(totalAmount)}</span>
            </p>
          </Panel>
          {delivery.data && <DeliveryTracking delivery={delivery.data} />}
        </div>

        <div className="flex flex-col gap-4">
          <Panel title="Actions">
            {nextStatuses.length === 0 && (
              <p className="text-sm text-stone-600">
                {status === 'CREATED' ? 'En attente du paiement en ligne du client.' : 'Aucune action possible : commande terminée.'}
              </p>
            )}
            <div className="flex flex-col gap-2.5">
              {nextStatuses.map((target) => (
                <button
                  key={target}
                  type="button"
                  disabled={changeStatus.isPending}
                  onClick={() => {
                    if (target !== 'CANCELLED' || window.confirm('Annuler cette commande ? Les articles seront remis en stock.')) {
                      changeStatus.mutate({ id, status: target }, { onSuccess: () => toast('Statut de la commande mis à jour.') })
                    }
                  }}
                  className={
                    target === 'CANCELLED'
                      ? 'rounded-full border-2 border-bissap/40 py-2.5 font-semibold text-bissap hover:bg-bissap-soft disabled:opacity-60'
                      : 'rounded-full bg-safran py-3 font-bold text-stone-900 hover:bg-safran-light disabled:opacity-60'
                  }
                >
                  {adminActionLabel(target, paymentMethod)}
                </button>
              ))}
            </div>
            <div aria-live="polite" className="mt-3">
              {changeStatus.isError && <ErrorMessage error={changeStatus.error} />}
            </div>
          </Panel>

          <Panel title="Livreur">
            <CourierSelect order={order.data} />
            <p className="mt-2 text-[13px] text-stone-600">Le client voit son livreur (nom et téléphone) dès que la commande est expédiée.</p>
          </Panel>

          <Panel title="Client">
            <p className="font-semibold">{customerName}</p>
            <p className="text-sm text-stone-600">{customerEmail}</p>
            <Link to={`/admin/commandes?q=${encodeURIComponent(customerEmail)}`} className="mt-2 inline-block text-sm font-semibold text-brand-700 underline underline-offset-3">
              Toutes ses commandes
            </Link>
          </Panel>

          <Panel title="Paiement">
            <PaymentCell order={order.data} />
          </Panel>
        </div>
      </div>
    </>
  )
}
