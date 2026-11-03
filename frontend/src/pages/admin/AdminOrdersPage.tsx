import { Link, useSearchParams } from 'react-router'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { useAdminOrders } from '../../hooks/useAdmin.ts'
import { formatDate, formatPrice } from '../../lib/format.ts'
import { ORDER_STATUSES, orderStatusLabel } from '../../lib/labels.ts'

export function AdminOrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const status = ORDER_STATUSES.find((s) => s === searchParams.get('statut'))
  const page = Number(searchParams.get('page') ?? 0)
  const orders = useAdminOrders(status, page)

  return (
    <>
      <div className="flex flex-col gap-1">
        <label htmlFor="statut" className="text-sm font-medium text-stone-700">
          Statut
        </label>
        <select
          id="statut"
          value={status ?? ''}
          onChange={(event) => setSearchParams(event.target.value ? { statut: event.target.value } : {})}
          className="w-64 rounded-md border border-stone-300 bg-white px-3 py-2"
        >
          <option value="">Tous</option>
          {ORDER_STATUSES.map((s) => (
            <option key={s} value={s}>
              {orderStatusLabel(s, 'ONLINE')}
            </option>
          ))}
        </select>
      </div>

      {orders.isError && <ErrorMessage error={orders.error} />}
      <div className="mt-4 overflow-x-auto rounded-lg border border-stone-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-stone-100 text-stone-700">
            <tr>
              <th scope="col" className="px-4 py-3">N°</th>
              <th scope="col" className="px-4 py-3">Date</th>
              <th scope="col" className="px-4 py-3">Client</th>
              <th scope="col" className="px-4 py-3">Paiement</th>
              <th scope="col" className="px-4 py-3">Statut</th>
              <th scope="col" className="px-4 py-3 text-right">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-200">
            {orders.data?.content.map((order) => (
              <tr key={order.id}>
                <td className="px-4 py-3">
                  <Link to={`/admin/commandes/${order.id}`} className="font-medium text-brand-700 hover:underline">
                    {order.id}
                  </Link>
                </td>
                <td className="px-4 py-3 text-stone-600">{formatDate(order.createdAt)}</td>
                <td className="px-4 py-3">{order.customerName}</td>
                <td className="px-4 py-3 text-stone-600">{order.paymentMethod === 'ONLINE' ? 'En ligne' : 'Espèces'}</td>
                <td className="px-4 py-3">{orderStatusLabel(order.status, order.paymentMethod)}</td>
                <td className="px-4 py-3 text-right font-semibold">{formatPrice(order.totalAmount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {orders.data?.content.length === 0 && <p className="p-6 text-center text-stone-600">Aucune commande.</p>}
      </div>
      {orders.data && (
        <Pagination
          page={orders.data.page}
          totalPages={orders.data.totalPages}
          onChange={(next) => setSearchParams({ ...(status ? { statut: status } : {}), page: String(next) })}
        />
      )}
    </>
  )
}
