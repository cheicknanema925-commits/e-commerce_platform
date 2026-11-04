import { type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { OrderStatusBadge } from '../../components/admin/OrderStatusBadge.tsx'
import { PageHeader } from '../../components/admin/PageHeader.tsx'
import { useAdminOrders, useStats } from '../../hooks/useAdmin.ts'
import { formatDate, formatPrice } from '../../lib/format.ts'
import { ORDER_VIEWS, findOrderView, orderViewCount } from '../../lib/orderViews.ts'

// Filtres dans l'URL (?vue=a-confirmer&q=diop&page=1) : le tableau de bord peut pointer directement sur un onglet
export function AdminOrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const view = findOrderView(searchParams.get('vue'))
  const q = searchParams.get('q') ?? undefined
  const page = Number(searchParams.get('page') ?? 0)
  const orders = useAdminOrders({ status: view.status, paymentMethod: view.paymentMethod, q, page })
  const stats = useStats()

  function update(changes: Record<string, string | undefined>) {
    const next = new URLSearchParams(searchParams)
    for (const [key, value] of Object.entries(changes)) {
      if (value) {
        next.set(key, value)
      } else {
        next.delete(key)
      }
    }
    next.delete('page')
    setSearchParams(next)
  }

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    update({ q: String(new FormData(event.currentTarget).get('q') ?? '').trim() })
  }

  return (
    <>
      <PageHeader title="Commandes" description="Confirmez, préparez, expédiez et livrez les commandes des clients." />

      <nav aria-label="Filtrer par étape" className="-mx-1 mb-4 flex gap-1 overflow-x-auto pb-1">
        {ORDER_VIEWS.map((v) => {
          const count = stats.data ? orderViewCount(v, stats.data) : null
          const selected = v.key === view.key
          return (
            <Link
              key={v.key}
              to={`?${new URLSearchParams({ ...(v.key !== 'toutes' ? { vue: v.key } : {}), ...(q ? { q } : {}) })}`}
              aria-current={selected ? 'page' : undefined}
              className={`flex shrink-0 items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium ${
                selected ? 'bg-stone-900 text-white' : 'text-stone-700 hover:bg-stone-200'
              }`}
            >
              {v.label}
              {count !== null && (
                <span
                  className={`rounded-full px-2 text-xs tabular-nums ${
                    v.actionable && count > 0 ? 'bg-amber-400 font-bold text-stone-900' : selected ? 'bg-stone-700' : 'bg-stone-200'
                  }`}
                >
                  {count}
                </span>
              )}
            </Link>
          )
        })}
      </nav>

      <form role="search" onSubmit={onSearch} className="mb-4 flex max-w-xl gap-2">
        <label htmlFor="recherche-commande" className="sr-only">
          Rechercher une commande
        </label>
        <input
          id="recherche-commande"
          name="q"
          type="search"
          key={q}
          defaultValue={q}
          maxLength={100}
          placeholder="N° de commande, nom ou email du client"
          className="flex-1 rounded-md border border-stone-300 bg-white px-3 py-2"
        />
        <button type="submit" className="rounded-md bg-stone-900 px-4 py-2 font-medium text-white hover:bg-stone-700">
          Rechercher
        </button>
      </form>

      {orders.isError && <ErrorMessage error={orders.error} />}
      <div className={`overflow-x-auto rounded-lg border border-stone-200 bg-white transition-opacity ${orders.isPlaceholderData ? 'opacity-60' : ''}`}>
        <table className="w-full text-left text-sm">
          <thead className="border-b border-stone-200 bg-stone-50 text-stone-600">
            <tr>
              <th scope="col" className="px-4 py-3 font-medium">Commande</th>
              <th scope="col" className="px-4 py-3 font-medium">Client</th>
              <th scope="col" className="px-4 py-3 font-medium">Articles</th>
              <th scope="col" className="px-4 py-3 font-medium">Paiement</th>
              <th scope="col" className="px-4 py-3 font-medium">Statut</th>
              <th scope="col" className="px-4 py-3 text-right font-medium">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {orders.data?.content.map((order) => (
              <tr key={order.id} className="hover:bg-stone-50">
                <td className="px-4 py-3">
                  <Link to={`/admin/commandes/${order.id}`} className="font-semibold text-brand-700 hover:underline">
                    n° {order.id}
                  </Link>
                  <p className="text-xs text-stone-500">{formatDate(order.createdAt)}</p>
                </td>
                <td className="px-4 py-3">
                  <p className="font-medium text-stone-900">{order.customerName}</p>
                  <p className="text-xs text-stone-500">{order.customerEmail}</p>
                </td>
                <td className="px-4 py-3 tabular-nums text-stone-700">{order.items.reduce((sum, item) => sum + item.quantity, 0)}</td>
                <td className="px-4 py-3 text-stone-700">{order.paymentMethod === 'ONLINE' ? 'En ligne' : 'Espèces'}</td>
                <td className="px-4 py-3">
                  <OrderStatusBadge status={order.status} paymentMethod={order.paymentMethod} />
                </td>
                <td className="px-4 py-3 text-right font-semibold tabular-nums">{formatPrice(order.totalAmount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {orders.data?.content.length === 0 && <p className="p-8 text-center text-stone-600">Aucune commande ne correspond.</p>}
      </div>
      {orders.data && (
        <Pagination
          page={orders.data.page}
          totalPages={orders.data.totalPages}
          onChange={(next) => {
            const params = new URLSearchParams(searchParams)
            params.set('page', String(next))
            setSearchParams(params)
          }}
        />
      )}
    </>
  )
}
