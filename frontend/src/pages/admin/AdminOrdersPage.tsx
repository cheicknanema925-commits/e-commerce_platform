import { type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { AdminOrder, OrderStatus } from '../../api/types.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { OrderStatusBadge } from '../../components/OrderStatusBadge.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { PaymentCell } from '../../components/admin/PaymentCell.tsx'
import { filterInput, Kpi, KpiStrip, PageHeader, TableCard, td, th } from '../../components/admin/ui.tsx'
import { useAdminOrders, useChangeOrderStatus, useStats } from '../../hooks/useAdmin.ts'
import { formatDate, formatNumber, formatPrice } from '../../lib/format.ts'
import { adminActionLabel } from '../../lib/labels.ts'
import { findOrderView, ORDER_VIEWS, orderViewCount, pendingActionCount } from '../../lib/orderViews.ts'
import { useToast } from '../../lib/toast.ts'

// Filtres dans l'URL (?vue=a-confirmer&paiement=CASH_ON_DELIVERY&q=diop) : le tableau de bord peut y renvoyer
export function AdminOrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const view = findOrderView(searchParams.get('vue'))
  const paymentParam = searchParams.get('paiement')
  const paymentMethod = view.paymentMethod ?? (paymentParam === 'ONLINE' || paymentParam === 'CASH_ON_DELIVERY' ? paymentParam : undefined)
  const q = searchParams.get('q') ?? undefined
  const page = Number(searchParams.get('page') ?? 0)
  const orders = useAdminOrders({ statuses: view.status ? [view.status] : undefined, paymentMethod, q, page })
  const stats = useStats()

  function update(name: string, value: string) {
    const next = new URLSearchParams(searchParams)
    if (value) {
      next.set(name, value)
    } else {
      next.delete(name)
    }
    next.delete('page')
    setSearchParams(next)
  }

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    update('q', String(new FormData(event.currentTarget).get('q') ?? '').trim())
  }

  const count = (status: OrderStatus) => stats.data?.ordersByStatus.find((s) => s.status === status)?.count ?? 0
  return (
    <>
      <PageHeader title="Suivi des commandes" description="Suivez les achats et mettez à jour leur statut de traitement." />

      {stats.data && (
        <KpiStrip>
          <Kpi label="Toutes les commandes" value={formatNumber(stats.data.orderCount)} detail="Depuis l’ouverture" />
          <Kpi label="À traiter" value={formatNumber(pendingActionCount(stats.data))} detail={<span className="font-semibold text-bissap">À confirmer, préparer ou livrer</span>} />
          <Kpi label="En livraison" value={formatNumber(count('SHIPPED'))} detail="En cours d’acheminement" />
          <Kpi label="Livrées" value={formatNumber(count('DELIVERED'))} detail={<span className="font-semibold text-kola">Commandes finalisées</span>} />
        </KpiStrip>
      )}

      <div className="flex flex-wrap gap-2.5">
        <form role="search" onSubmit={onSearch} className="flex flex-1 basis-full sm:flex-none sm:basis-auto">
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
            className={`${filterInput} w-full sm:w-80`}
          />
        </form>
        <select aria-label="Statut" value={view.key === 'toutes' ? '' : view.key} onChange={(event) => update('vue', event.target.value)} className={filterInput}>
          {ORDER_VIEWS.map((v) => (
            <option key={v.key} value={v.key === 'toutes' ? '' : v.key}>
              {v.key === 'toutes' ? 'Tous les statuts' : v.label}
              {stats.data && ` (${orderViewCount(v, stats.data)})`}
            </option>
          ))}
        </select>
        <select
          aria-label="Moyen de paiement"
          value={paymentMethod ?? ''}
          disabled={view.paymentMethod !== undefined}
          onChange={(event) => update('paiement', event.target.value)}
          className={filterInput}
        >
          <option value="">Tous les paiements</option>
          <option value="ONLINE">En ligne (Wave, Orange Money, carte)</option>
          <option value="CASH_ON_DELIVERY">Espèces à la livraison</option>
        </select>
      </div>

      {orders.isError && <ErrorMessage error={orders.error} />}
      <OrdersTable orders={orders.data?.content} total={orders.data?.totalElements} dimmed={orders.isPlaceholderData} />
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

function OrdersTable({ orders, total, dimmed }: { orders?: AdminOrder[]; total?: number; dimmed: boolean }) {
  const changeStatus = useChangeOrderStatus()
  const toast = useToast()

  function onChange(order: AdminOrder, target: OrderStatus) {
    if (target === 'CANCELLED' && !window.confirm(`Annuler la commande n° ${order.id} ? Les articles seront remis en stock.`)) {
      return
    }
    changeStatus.mutate(
      { id: order.id, status: target },
      {
        onSuccess: (updated) => {
          const cash = updated.paymentMethod === 'CASH_ON_DELIVERY' && target === 'DELIVERED'
          toast(cash ? `Commande n° ${order.id} livrée : ${formatPrice(order.totalAmount)} en espèces encaissés.` : `Commande n° ${order.id} mise à jour.`)
        },
        onError: (error) => toast(error.message),
      },
    )
  }

  return (
    <TableCard title="Liste des commandes" count={total}>
      <table className={`w-full text-sm transition-opacity ${dimmed ? 'opacity-60' : ''}`}>
        <thead>
          <tr>
            <th scope="col" className={th}>Commande</th>
            <th scope="col" className={th}>Client</th>
            <th scope="col" className={th}>Date</th>
            <th scope="col" className={`${th} text-right`}>Articles</th>
            <th scope="col" className={`${th} text-right`}>Total</th>
            <th scope="col" className={th}>Paiement</th>
            <th scope="col" className={th}>Statut</th>
            <th scope="col" className={th}>Changer le statut</th>
          </tr>
        </thead>
        <tbody>
          {orders?.map((order) => (
            <tr key={order.id} className="hover:bg-stone-50">
              <td className={td}>
                <Link to={`/admin/commandes/${order.id}`} className="font-semibold text-brand-700 hover:underline">
                  n° {order.id}
                </Link>
              </td>
              <td className={td}>
                <span className="block font-medium">{order.customerName}</span>
                <span className="text-xs text-stone-600">{order.customerEmail}</span>
              </td>
              <td className={td}>{formatDate(order.createdAt)}</td>
              <td className={`${td} text-right tabular-nums`}>{order.items.reduce((sum, item) => sum + item.quantity, 0)}</td>
              <td className={`${td} text-right font-semibold tabular-nums`}>{formatPrice(order.totalAmount)}</td>
              <td className={td}>
                <PaymentCell order={order} />
              </td>
              <td className={td}>
                <OrderStatusBadge status={order.status} paymentMethod={order.paymentMethod} />
              </td>
              <td className={td}>
                {/* Seules les transitions permises par la machine à états du serveur sont proposées */}
                {order.nextStatuses.length === 0 ? (
                  <span className="text-stone-500">{order.status === 'CREATED' ? 'Attente du paiement' : '—'}</span>
                ) : (
                  <>
                    <label htmlFor={`statut-${order.id}`} className="sr-only">
                      Changer le statut de la commande n° {order.id}
                    </label>
                    <select
                      id={`statut-${order.id}`}
                      value=""
                      disabled={changeStatus.isPending}
                      onChange={(event) => onChange(order, event.target.value as OrderStatus)}
                      className="rounded-lg border border-stone-200 bg-white px-2.5 py-1.5 text-[13px] font-medium"
                    >
                      <option value="" disabled>
                        Choisir une action…
                      </option>
                      {order.nextStatuses.map((target) => (
                        <option key={target} value={target}>
                          {adminActionLabel(target, order.paymentMethod)}
                        </option>
                      ))}
                    </select>
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {orders?.length === 0 && <p className="p-9 text-center text-stone-600">Aucune commande ne correspond à ces filtres.</p>}
    </TableCard>
  )
}
