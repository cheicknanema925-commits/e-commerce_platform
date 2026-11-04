import { Link } from 'react-router'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { OrderStatusBadge } from '../components/OrderStatusBadge.tsx'
import { useMyOrders } from '../hooks/useOrders.ts'
import { formatDate, formatPrice } from '../lib/format.ts'

export function OrdersPage() {
  const orders = useMyOrders()

  return (
    <>
      <h1 className="text-[clamp(28px,3.4vw,40px)] font-bold tracking-tight">Mes commandes</h1>
      <p className="mt-1 text-stone-600">Suivez la préparation et la livraison de vos commandes.</p>
      {orders.isPending && <p className="mt-6 text-stone-600">Chargement…</p>}
      {orders.isError && <ErrorMessage error={orders.error} />}
      {orders.data?.length === 0 && (
        <div className="mt-6 rounded-[18px] border border-dashed border-stone-300 bg-white p-10 text-center text-stone-600">
          <p>Vous n'avez pas encore passé de commande.</p>
          <Link to="/" className="mt-4 inline-block rounded-full bg-safran px-6 py-3 font-bold text-stone-900 hover:bg-safran-light">
            Voir les produits
          </Link>
        </div>
      )}
      <ul className="mt-6 space-y-3">
        {orders.data?.map((order) => {
          const count = order.items.reduce((sum, item) => sum + item.quantity, 0)
          return (
            <li key={order.id}>
              <Link
                to={`/commandes/${order.id}`}
                className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-stone-200 bg-white p-5 hover:border-brand-700"
              >
                <span>
                  <span className="font-semibold">Commande n° {order.id}</span>
                  <span className="block text-sm text-stone-600">
                    {formatDate(order.createdAt)} · {count} article{count > 1 ? 's' : ''}
                  </span>
                </span>
                <OrderStatusBadge status={order.status} paymentMethod={order.paymentMethod} />
                <span className="font-bold">{formatPrice(order.totalAmount)}</span>
              </Link>
            </li>
          )
        })}
      </ul>
    </>
  )
}
