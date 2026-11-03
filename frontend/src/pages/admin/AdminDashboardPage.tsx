import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { useStats } from '../../hooks/useAdmin.ts'
import { formatPrice } from '../../lib/format.ts'
import { orderStatusLabel } from '../../lib/labels.ts'

export function AdminDashboardPage() {
  const stats = useStats()

  if (stats.isError) {
    return <ErrorMessage error={stats.error} />
  }
  if (!stats.data) {
    return <p className="text-stone-600">Chargement du tableau de bord…</p>
  }

  const { collectedRevenue, orderCount, customerCount, ordersByStatus, bestSellers, lowStock } = stats.data
  return (
    <div className="flex flex-col gap-6">
      <dl className="grid gap-4 sm:grid-cols-3">
        <StatCard label="Encaissé" value={formatPrice(collectedRevenue)} hint="Paiements en ligne confirmés et espèces reçues" />
        <StatCard label="Commandes" value={String(orderCount)} />
        <StatCard label="Clients" value={String(customerCount)} />
      </dl>

      <div className="grid gap-6 lg:grid-cols-3">
        <Panel title="Commandes par statut">
          <ul className="space-y-2">
            {ordersByStatus.map(({ status, count }) => (
              <li key={status} className="flex justify-between">
                <Link to={`/admin/commandes?statut=${status}`} className="text-brand-700 hover:underline">
                  {orderStatusLabel(status, 'ONLINE')}
                </Link>
                <span className="font-semibold">{count}</span>
              </li>
            ))}
          </ul>
        </Panel>

        <Panel title="Meilleures ventes">
          {bestSellers.length === 0 ? (
            <p className="text-sm text-stone-600">Aucune vente pour l'instant.</p>
          ) : (
            <ol className="list-inside list-decimal space-y-2">
              {bestSellers.map((product) => (
                <li key={product.productId}>
                  {product.name} <span className="text-stone-600">({product.quantity} vendus)</span>
                </li>
              ))}
            </ol>
          )}
        </Panel>

        <Panel title="Stock faible (5 ou moins)">
          {lowStock.length === 0 ? (
            <p className="text-sm text-stone-600">Tous les produits sont bien approvisionnés.</p>
          ) : (
            <ul className="space-y-2">
              {lowStock.map((product) => (
                <li key={product.productId} className="flex justify-between gap-2">
                  <Link to={`/admin/produits/${product.productId}`} className="text-brand-700 hover:underline">
                    {product.name}
                  </Link>
                  <span className={product.stock === 0 ? 'font-semibold text-red-700' : 'font-semibold text-amber-700'}>
                    {product.stock}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>
    </div>
  )
}

function StatCard({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="rounded-lg border border-stone-200 bg-white p-5">
      <dt className="text-sm font-medium text-stone-600">{label}</dt>
      <dd className="mt-1 text-2xl font-bold">{value}</dd>
      {hint && <dd className="mt-1 text-xs text-stone-500">{hint}</dd>}
    </div>
  )
}

function Panel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-stone-200 bg-white p-5">
      <h2 className="mb-4 font-semibold">{title}</h2>
      {children}
    </section>
  )
}
