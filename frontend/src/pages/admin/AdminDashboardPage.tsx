import { useState } from 'react'
import { Link } from 'react-router'
import type { SalesPeriod } from '../../api/types.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { OrderStatusBadge } from '../../components/OrderStatusBadge.tsx'
import { OrdersToDo } from '../../components/admin/OrdersToDo.tsx'
import { ExportButton } from '../../components/admin/ExportButton.tsx'
import { PaymentCell } from '../../components/admin/PaymentCell.tsx'
import { SalesPanel } from '../../components/admin/SalesPanel.tsx'
import { AmountKpi, BarList, btnGhost, Kpi, KpiStrip, PageHeader, Panel, TableCard, td, th } from '../../components/admin/ui.tsx'
import { useAdminOrders, useStats } from '../../hooks/useAdmin.ts'
import { formatDate, formatNumber, formatPrice } from '../../lib/format.ts'
import { pendingActionCount } from '../../lib/orderViews.ts'

const todayLabel = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date())

export function AdminDashboardPage() {
  const stats = useStats()
  const recent = useAdminOrders({ page: 0, size: 5 })
  const [period, setPeriod] = useState<SalesPeriod>('DAY')

  if (stats.isError) {
    return <ErrorMessage error={stats.error} />
  }
  if (!stats.data) {
    return <p className="text-stone-600">Chargement de la vue d'ensemble…</p>
  }

  const data = stats.data
  const pending = pendingActionCount(data)
  return (
    <>
      <PageHeader
        title="Vue d’ensemble"
        description={`L’activité de votre boutique, aujourd’hui ${todayLabel}.`}
        actions={
          <>
            <ExportButton name="orders" label="Exporter le rapport" />
            <Link to="/admin/produits/nouveau" className={btnGhost}>
              Ajouter un article
            </Link>
          </>
        }
      />

      <KpiStrip>
        <AmountKpi label="Encaissé ce mois-ci" amount={data.thisMonth.collected} detail={`${data.thisMonth.orders} commandes ce mois-ci`} />
        <Kpi label="Commandes" value={formatNumber(data.orderCount)} detail={pending > 0 ? <span className="font-semibold text-bissap">{pending} à traiter</span> : 'Tout est à jour'} />
        <Kpi label="Produits en catalogue" value={formatNumber(data.catalog.productCount)} detail={`${data.catalog.outOfStock} en rupture`} />
        <AmountKpi
          label="Espèces à encaisser"
          amount={data.payments.cashToCollect}
          detail={`${data.payments.cashToCollectOrders} commande${data.payments.cashToCollectOrders > 1 ? 's' : ''} payée${data.payments.cashToCollectOrders > 1 ? 's' : ''} à la livraison`}
          accent
        />
      </KpiStrip>

      <OrdersToDo stats={data} />

      <div className="grid gap-4 xl:grid-cols-[1.55fr_1fr]">
        <SalesPanel title="Évolution des encaissements" period={period} onPeriodChange={setPeriod} />
        <Panel title="Ventes par catégorie">
          {data.salesByCategory.length === 0 ? (
            <p className="text-sm text-stone-600">Aucune vente pour l'instant.</p>
          ) : (
            <BarList rows={data.salesByCategory.map((c) => ({ label: c.category, value: c.revenue, display: formatPrice(c.revenue) }))} />
          )}
          <p className="mt-4 text-[13px] text-stone-600">Montant des articles commandés, commandes annulées exclues.</p>
        </Panel>
      </div>

      <TableCard
        title="Commandes récentes"
        action={
          <Link to="/admin/commandes" className={btnGhost}>
            Voir toutes les commandes
          </Link>
        }
      >
        {recent.isError && <ErrorMessage error={recent.error} />}
        <table className="w-full text-sm">
          <thead>
            <tr>
              <th scope="col" className={th}>N° commande</th>
              <th scope="col" className={th}>Client</th>
              <th scope="col" className={th}>Date</th>
              <th scope="col" className={`${th} text-right`}>Montant</th>
              <th scope="col" className={th}>Paiement</th>
              <th scope="col" className={th}>Statut</th>
            </tr>
          </thead>
          <tbody>
            {recent.data?.content.map((order) => (
              <tr key={order.id} className="hover:bg-stone-50">
                <td className={td}>
                  <Link to={`/admin/commandes/${order.id}`} className="font-semibold text-brand-700 hover:underline">
                    n° {order.id}
                  </Link>
                </td>
                <td className={td}>{order.customerName}</td>
                <td className={td}>{formatDate(order.createdAt)}</td>
                <td className={`${td} text-right font-semibold tabular-nums`}>{formatPrice(order.totalAmount)}</td>
                <td className={td}>
                  <PaymentCell order={order} />
                </td>
                <td className={td}>
                  <OrderStatusBadge status={order.status} paymentMethod={order.paymentMethod} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {recent.data?.content.length === 0 && <p className="p-9 text-center text-stone-600">Aucune commande pour l'instant.</p>}
      </TableCard>
    </>
  )
}
