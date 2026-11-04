import { Link, useSearchParams } from 'react-router'
import type { OrderStatus } from '../../api/types.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { Icon } from '../../components/Icon.tsx'
import { OrderStatusBadge } from '../../components/OrderStatusBadge.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { AmountKpi, BarList, btnGhost, Kpi, KpiStrip, PageHeader, Panel, TableCard, td, th } from '../../components/admin/ui.tsx'
import { useAdminOrders, useStats } from '../../hooks/useAdmin.ts'
import { cashState } from '../../lib/admin.ts'
import { formatNumber, formatPrice } from '../../lib/format.ts'

// Filtre du tableau des commandes en espèces : chaque choix correspond à un ou plusieurs statuts
const CASH_FILTERS: Record<string, { label: string; statuses?: OrderStatus[] }> = {
  '': { label: 'Toutes' },
  due: { label: 'À encaisser', statuses: ['CREATED', 'PREPARING', 'SHIPPED'] },
  collected: { label: 'Encaissées', statuses: ['DELIVERED'] },
  cancelled: { label: 'Annulées', statuses: ['CANCELLED'] },
}

// Les espèces sont enregistrées comme payées quand l'admin marque la commande « livrée » (le livreur a remis l'argent)
export function AdminPaymentsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const filterKey = searchParams.get('encaissement') ?? ''
  const filter = CASH_FILTERS[filterKey] ?? CASH_FILTERS['']
  const page = Number(searchParams.get('page') ?? 0)
  const stats = useStats()
  const cashOrders = useAdminOrders({ paymentMethod: 'CASH_ON_DELIVERY', statuses: filter.statuses, page })
  const allCash = useAdminOrders({ paymentMethod: 'CASH_ON_DELIVERY', page: 0, size: 1 })

  if (stats.isError) {
    return <ErrorMessage error={stats.error} />
  }
  if (!stats.data) {
    return <p className="text-stone-600">Chargement…</p>
  }
  const { payments, orderCount } = stats.data
  const cashCount = allCash.data?.totalElements ?? 0

  return (
    <>
      <PageHeader
        title="Paiements"
        description="Suivez les encaissements par moyen de paiement, dont les espèces remises par les livreurs."
        actions={
          <Link to="/admin/commandes?paiement=CASH_ON_DELIVERY" className={btnGhost}>
            Commandes en espèces
          </Link>
        }
      />

      <div className="mb-4.5 flex items-start gap-4 rounded-lg border border-safran/50 bg-safran-soft px-4.5 py-4">
        <Icon name="cash" className="size-7" />
        <div>
          <b className="block">Espèces à récupérer auprès des livreurs</b>
          <p className="mt-0.5 text-sm text-stone-700">
            {payments.cashToCollectOrders > 0
              ? `${formatPrice(payments.cashToCollect)} sur ${payments.cashToCollectOrders} commande${payments.cashToCollectOrders > 1 ? 's' : ''} pas encore livrée${payments.cashToCollectOrders > 1 ? 's' : ''}. Marquez chaque commande « livrée » quand le livreur vous remet l’argent : l’encaissement est alors enregistré.`
              : 'Aucune somme en attente : tous les paiements en espèces ont été remis.'}
          </p>
        </div>
      </div>

      <KpiStrip>
        <AmountKpi label="Encaissé en ligne" amount={payments.collectedOnline} detail="Wave, Orange Money et carte" />
        <AmountKpi label="Espèces à encaisser" amount={payments.cashToCollect} detail={`${payments.cashToCollectOrders} commande(s) en cours`} accent />
        <AmountKpi label="Espèces encaissées" amount={payments.collectedCash} detail="Remises par les livreurs" />
        <Kpi label="Commandes en espèces" value={formatNumber(cashCount)} detail={`sur ${formatNumber(orderCount)} commandes au total`} />
      </KpiStrip>

      <div className="grid items-start gap-4 xl:grid-cols-[1.55fr_1fr]">
        <div className="min-w-0">
          <TableCard
            title="Commandes payées à la livraison"
            count={cashOrders.data?.totalElements}
            action={
              <select
                aria-label="Filtrer l'encaissement"
                value={filterKey}
                onChange={(event) => setSearchParams(event.target.value ? { encaissement: event.target.value } : {})}
                className="rounded-lg border border-stone-200 bg-white px-2.5 py-1.5 text-sm"
              >
                {Object.entries(CASH_FILTERS).map(([key, value]) => (
                  <option key={key} value={key}>
                    {value.label}
                  </option>
                ))}
              </select>
            }
          >
            {cashOrders.isError && <ErrorMessage error={cashOrders.error} />}
            <table className={`w-full text-sm ${cashOrders.isPlaceholderData ? 'opacity-60' : ''}`}>
              <thead>
                <tr>
                  <th scope="col" className={th}>Commande</th>
                  <th scope="col" className={th}>Client</th>
                  <th scope="col" className={`${th} text-right`}>Montant</th>
                  <th scope="col" className={th}>Statut</th>
                  <th scope="col" className={th}>Encaissement</th>
                </tr>
              </thead>
              <tbody>
                {cashOrders.data?.content.map((order) => {
                  const state = cashState(order)
                  return (
                    <tr key={order.id} className="hover:bg-stone-50">
                      <td className={td}>
                        <Link to={`/admin/commandes/${order.id}`} className="font-semibold text-brand-700 hover:underline">
                          n° {order.id}
                        </Link>
                      </td>
                      <td className={td}>{order.customerName}</td>
                      <td className={`${td} text-right font-semibold tabular-nums`}>{formatPrice(order.totalAmount)}</td>
                      <td className={td}>
                        <OrderStatusBadge status={order.status} paymentMethod={order.paymentMethod} />
                      </td>
                      <td className={td}>
                        {state === 'collected' && <span className="font-semibold text-kola">Encaissé</span>}
                        {state === 'due' && <span className="font-semibold text-bissap">À encaisser à la livraison</span>}
                        {state === 'cancelled' && <span className="text-stone-500">Sans objet</span>}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            {cashOrders.data?.content.length === 0 && <p className="p-9 text-center text-stone-600">Aucune commande payée à la livraison pour ce filtre.</p>}
          </TableCard>
          {cashOrders.data && (
            <Pagination
              page={cashOrders.data.page}
              totalPages={cashOrders.data.totalPages}
              onChange={(next) => setSearchParams({ ...(filterKey ? { encaissement: filterKey } : {}), page: String(next) })}
            />
          )}
        </div>

        <div className="mt-4.5">
          <Panel title="Répartition des encaissements">
            <BarList
              rows={[
                { label: 'En ligne (Wave, Orange Money, carte)', value: payments.collectedOnline, display: formatPrice(payments.collectedOnline) },
                { label: 'Espèces à la livraison', value: payments.collectedCash, display: formatPrice(payments.collectedCash) },
              ]}
            />
            <p className="mt-4 text-[13px] text-stone-600">
              Le détail par opérateur (Wave, Orange Money, carte) sera disponible avec l’intégration réelle de PayDunya.
            </p>
          </Panel>
        </div>
      </div>
    </>
  )
}
