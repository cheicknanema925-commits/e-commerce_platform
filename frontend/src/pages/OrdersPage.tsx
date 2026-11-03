import { Link } from 'react-router'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { useMyOrders } from '../hooks/useOrders.ts'
import { formatDate, formatPrice } from '../lib/format.ts'
import { orderStatusLabel } from '../lib/labels.ts'

export function OrdersPage() {
  const orders = useMyOrders()

  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight">Mes commandes</h1>
      {orders.isPending && <p className="mt-6 text-stone-600">Chargement…</p>}
      {orders.isError && <ErrorMessage error={orders.error} />}
      {orders.data?.length === 0 && (
        <p className="mt-6 rounded-lg border border-dashed border-stone-300 p-8 text-center text-stone-600">
          Vous n'avez pas encore passé de commande.
        </p>
      )}
      <ul className="mt-6 space-y-3">
        {orders.data?.map((order) => (
          <li key={order.id}>
            <Link
              to={`/commandes/${order.id}`}
              className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-stone-200 bg-white p-4 hover:border-brand-700"
            >
              <span>
                <span className="font-semibold">Commande n° {order.id}</span>
                <span className="block text-sm text-stone-600">{formatDate(order.createdAt)}</span>
              </span>
              <span className="text-sm font-medium text-stone-700">{orderStatusLabel(order.status, order.paymentMethod)}</span>
              <span className="font-bold text-brand-800">{formatPrice(order.totalAmount)}</span>
            </Link>
          </li>
        ))}
      </ul>
    </>
  )
}
