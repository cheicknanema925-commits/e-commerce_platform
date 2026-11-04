import { type ReactNode, useState } from 'react'
import { Link } from 'react-router'
import type { SalesPeriod } from '../../api/types.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { PageHeader } from '../../components/admin/PageHeader.tsx'
import { SalesChart } from '../../components/admin/SalesChart.tsx'
import { useSales, useStats } from '../../hooks/useAdmin.ts'
import { formatPrice } from '../../lib/format.ts'
import { orderStatusLabel } from '../../lib/labels.ts'
import { ORDER_VIEWS, orderViewCount } from '../../lib/orderViews.ts'

const todayLabel = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'full' }).format(new Date())

export function AdminDashboardPage() {
  const stats = useStats()
  const [period, setPeriod] = useState<SalesPeriod>('DAY')
  const sales = useSales(period)

  if (stats.isError) {
    return <ErrorMessage error={stats.error} />
  }
  if (!stats.data) {
    return <p className="text-stone-600">Chargement du tableau de bord…</p>
  }

  const data = stats.data
  const toDo = ORDER_VIEWS.filter((view) => view.actionable)
    .map((view) => ({ view, count: orderViewCount(view, data) }))
    .filter(({ count }) => count > 0)
  const maxStatusCount = Math.max(1, ...data.ordersByStatus.map((s) => s.count))

  return (
    <>
      <PageHeader title="Tableau de bord" description={<span className="first-letter:uppercase">{todayLabel}</span>} />

      <section aria-labelledby="a-traiter" className="mb-6 rounded-lg border border-amber-200 bg-amber-50 p-4">
        <h2 id="a-traiter" className="font-semibold text-amber-950">
          À traiter
        </h2>
        {toDo.length === 0 ? (
          <p className="mt-1 text-sm text-amber-900">Rien en attente : toutes les commandes sont à jour.</p>
        ) : (
          <ul className="mt-2 flex flex-wrap gap-2">
            {toDo.map(({ view, count }) => (
              <li key={view.key}>
                <Link
                  to={`/admin/commandes?vue=${view.key}`}
                  className="inline-flex items-center gap-2 rounded-md border border-amber-300 bg-white px-3 py-1.5 text-sm font-medium text-stone-900 hover:bg-amber-100"
                >
                  <span className="rounded-full bg-amber-400 px-2 text-xs font-bold">{count}</span>
                  {view.label}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <dl className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatTile label="Encaissé aujourd'hui" value={formatPrice(data.today.collected)} detail={ordersText(data.today.orders)} />
        <StatTile label="Encaissé ce mois-ci" value={formatPrice(data.thisMonth.collected)} detail={ordersText(data.thisMonth.orders)} />
        <StatTile label="Encaissé au total" value={formatPrice(data.collectedRevenue)} detail={`${data.orderCount} commandes au total`} />
        <StatTile
          label="Clients"
          value={String(data.customerCount)}
          detail={
            <Link to="/admin/clients" className="text-brand-700 hover:underline">
              Voir les clients
            </Link>
          }
        />
      </dl>

      <section aria-labelledby="ventes" className="mt-6">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 id="ventes" className="font-semibold text-stone-900">
              Montant encaissé
            </h2>
            <p className="text-sm text-stone-600">
              En francs CFA, par {period === 'DAY' ? 'jour sur les 30 derniers jours' : 'mois sur les 12 derniers mois'}
            </p>
          </div>
          <div role="group" aria-label="Période du graphique" className="inline-flex rounded-md border border-stone-300 bg-white p-0.5">
            {(['DAY', 'MONTH'] as const).map((value) => (
              <button
                key={value}
                type="button"
                aria-pressed={period === value}
                onClick={() => setPeriod(value)}
                className={`rounded px-3 py-1.5 text-sm font-medium ${period === value ? 'bg-stone-900 text-white' : 'text-stone-700 hover:bg-stone-100'}`}
              >
                {value === 'DAY' ? '30 jours' : '12 mois'}
              </button>
            ))}
          </div>
        </div>
        <div className="rounded-lg border border-stone-200 bg-white p-4">
          {sales.isError && <ErrorMessage error={sales.error} />}
          {sales.data ? (
            <SalesChart report={sales.data} dimmed={sales.isPlaceholderData} />
          ) : (
            !sales.isError && <p className="py-24 text-center text-sm text-stone-500">Chargement du graphique…</p>
          )}
        </div>
      </section>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Panel title="Commandes par statut">
          {data.ordersByStatus.length === 0 ? (
            <p className="text-sm text-stone-600">Aucune commande pour l'instant.</p>
          ) : (
            <ul className="space-y-3">
              {data.ordersByStatus.map(({ status, count }) => (
                <li key={status}>
                  <div className="flex justify-between text-sm">
                    <span className="text-stone-700">{orderStatusLabel(status, 'ONLINE')}</span>
                    <span className="font-semibold tabular-nums">{count}</span>
                  </div>
                  <div aria-hidden="true" className="mt-1 h-2 rounded-full bg-stone-100">
                    <div className="h-2 rounded-full bg-brand-600" style={{ width: `${(count / maxStatusCount) * 100}%` }} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Panel>

        <Panel
          title="Meilleures ventes"
          action={
            <Link to="/admin/produits" className="text-sm font-medium text-brand-700 hover:underline">
              Produits
            </Link>
          }
        >
          {data.bestSellers.length === 0 ? (
            <p className="text-sm text-stone-600">Aucune vente pour l'instant.</p>
          ) : (
            <ol className="space-y-2 text-sm">
              {data.bestSellers.map((product, index) => (
                <li key={product.productId} className="flex items-baseline gap-3">
                  <span className="w-4 text-stone-500 tabular-nums">{index + 1}</span>
                  <Link to={`/admin/produits/${product.productId}`} className="flex-1 truncate text-stone-900 hover:underline">
                    {product.name}
                  </Link>
                  <span className="text-stone-600 tabular-nums">{product.quantity} vendus</span>
                </li>
              ))}
            </ol>
          )}
        </Panel>

        <Panel title="Stock">
          <dl className="grid grid-cols-3 gap-2 text-center">
            <MiniFigure label="Produits" value={String(data.catalog.productCount)} />
            <MiniFigure label="En rupture" value={String(data.catalog.outOfStock)} />
            <MiniFigure label="Valeur" value={formatPrice(data.catalog.stockValue)} />
          </dl>
          <h3 className="mt-4 text-sm font-medium text-stone-700">Bientôt en rupture (5 ou moins)</h3>
          {data.lowStock.length === 0 ? (
            <p className="mt-1 text-sm text-stone-600">Tous les produits sont bien approvisionnés.</p>
          ) : (
            <ul className="mt-2 space-y-1.5 text-sm">
              {data.lowStock.map((product) => (
                <li key={product.productId} className="flex justify-between gap-2">
                  <Link to={`/admin/produits/${product.productId}`} className="truncate text-brand-700 hover:underline">
                    {product.name}
                  </Link>
                  <span className={`font-semibold tabular-nums ${product.stock === 0 ? 'text-red-700' : 'text-amber-700'}`}>
                    {product.stock === 0 ? 'Rupture' : product.stock}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>
    </>
  )
}

function ordersText(count: number): string {
  return `${count} commande${count > 1 ? 's' : ''} passée${count > 1 ? 's' : ''}`
}

function StatTile({ label, value, detail }: { label: string; value: string; detail: ReactNode }) {
  return (
    <div className="rounded-lg border border-stone-200 bg-white p-5">
      <dt className="text-sm font-medium text-stone-600">{label}</dt>
      <dd className="mt-1 text-2xl font-semibold text-stone-900">{value}</dd>
      <dd className="mt-1 text-sm text-stone-500">{detail}</dd>
    </div>
  )
}

function MiniFigure({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md bg-stone-50 px-2 py-3">
      <dt className="text-xs text-stone-600">{label}</dt>
      <dd className="mt-0.5 truncate text-sm font-semibold text-stone-900">{value}</dd>
    </div>
  )
}

function Panel({ title, action, children }: { title: string; action?: ReactNode; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-stone-200 bg-white p-5">
      <div className="mb-4 flex items-center justify-between gap-2">
        <h2 className="font-semibold text-stone-900">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  )
}
