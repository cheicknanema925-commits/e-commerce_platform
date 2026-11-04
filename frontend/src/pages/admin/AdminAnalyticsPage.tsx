import { useState } from 'react'
import { Link } from 'react-router'
import type { SalesPeriod } from '../../api/types.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { SalesPanel } from '../../components/admin/SalesPanel.tsx'
import { AmountKpi, BarList, Kpi, KpiStrip, Meter, PageHeader, Panel } from '../../components/admin/ui.tsx'
import { useStats } from '../../hooks/useAdmin.ts'
import { percent } from '../../lib/admin.ts'
import { formatNumber, formatPrice } from '../../lib/format.ts'

export function AdminAnalyticsPage() {
  const stats = useStats()
  const [period, setPeriod] = useState<SalesPeriod>('MONTH')

  if (stats.isError) {
    return <ErrorMessage error={stats.error} />
  }
  if (!stats.data) {
    return <p className="text-stone-600">Chargement…</p>
  }
  const data = stats.data
  const cancelled = data.ordersByStatus.find((s) => s.status === 'CANCELLED')?.count ?? 0
  const kept = data.orderCount - cancelled
  const bestMax = Math.max(1, ...data.bestSellers.map((p) => p.quantity))

  return (
    <>
      <PageHeader title="Analyse des ventes" description="Indicateurs de performance et tendances commerciales, calculés à partir des commandes réelles." />

      <KpiStrip>
        <AmountKpi label="Encaissé ce mois-ci" amount={data.thisMonth.collected} detail={`${data.thisMonth.orders} commandes ce mois-ci`} />
        <AmountKpi label="Panier moyen" amount={data.averageOrderValue} detail="Par commande non annulée" />
        <Kpi label="Taux d’annulation" value={percent(cancelled, data.orderCount)} detail={`${cancelled} commande(s) annulée(s)`} />
        <Kpi label="Clients fidèles" value={percent(data.customers.repeat, data.customerCount)} detail="Au moins 2 commandes" />
      </KpiStrip>

      <div className="grid gap-4 xl:grid-cols-[1.55fr_1fr]">
        <SalesPanel title="Ventes encaissées" period={period} onPeriodChange={setPeriod} />
        <Panel title="Performance par catégorie">
          {data.salesByCategory.length === 0 ? (
            <p className="text-sm text-stone-600">Aucune vente pour l'instant.</p>
          ) : (
            <BarList
              rows={data.salesByCategory.map((c) => ({ label: `${c.category} · ${c.quantity} article(s)`, value: c.revenue, display: formatPrice(c.revenue) }))}
            />
          )}
        </Panel>
      </div>

      <div className="mt-4 grid gap-4 xl:grid-cols-[1.55fr_1fr]">
        <Panel title="Meilleures ventes">
          {data.bestSellers.length === 0 ? (
            <p className="text-sm text-stone-600">Aucune vente pour l'instant.</p>
          ) : (
            <ol>
              {data.bestSellers.map((product, index) => (
                <li key={product.productId} className={`flex items-center gap-3 py-2.5 ${index > 0 ? 'border-t border-stone-200' : ''}`}>
                  {product.imageUrl ? (
                    <img src={product.imageUrl} alt="" className="size-10 shrink-0 rounded-lg object-cover" />
                  ) : (
                    <span aria-hidden="true" className="grid size-10 shrink-0 place-items-center rounded-lg bg-brand-100 font-bold text-brand-700">
                      {index + 1}
                    </span>
                  )}
                  <div className="min-w-0 flex-1">
                    <Link to={`/admin/produits/${product.productId}`} className="block truncate font-semibold hover:underline">
                      {product.name}
                    </Link>
                    <span className="text-[13px] text-stone-600">{formatNumber(product.quantity)} vendus</span>
                  </div>
                  <div aria-hidden="true" className="h-2 w-28 overflow-hidden rounded-full bg-stone-100">
                    <div className="h-full rounded-full bg-safran" style={{ width: `${(product.quantity / bestMax) * 100}%` }} />
                  </div>
                </li>
              ))}
            </ol>
          )}
        </Panel>
        <Panel title="Résumé commercial">
          <Meter label="Commandes non annulées" value={formatNumber(kept)} ratio={data.orderCount ? kept / data.orderCount : 0} tone="kola" />
          <Meter label="Commandes annulées" value={formatNumber(cancelled)} ratio={data.orderCount ? cancelled / data.orderCount : 0} tone="bissap" />
          <Meter label="Clients fidèles" value={percent(data.customers.repeat, data.customerCount)} ratio={data.customerCount ? data.customers.repeat / data.customerCount : 0} tone="brand" />
          <p className="mt-4 text-[13px] text-stone-600">
            {data.customers.newThisMonth} nouveau(x) client(s) ce mois-ci, {formatNumber(data.customerCount)} au total.
          </p>
        </Panel>
      </div>
    </>
  )
}
