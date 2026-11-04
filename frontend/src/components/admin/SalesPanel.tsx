import type { SalesPeriod } from '../../api/types.ts'
import { useSales } from '../../hooks/useAdmin.ts'
import { ErrorMessage } from '../ErrorMessage.tsx'
import { SalesChart } from './SalesChart.tsx'
import { Panel } from './ui.tsx'

// Panneau du graphique des encaissements, avec le choix de la période dans son en-tête (comme la maquette)
export function SalesPanel({ title, period, onPeriodChange }: { title: string; period: SalesPeriod; onPeriodChange: (period: SalesPeriod) => void }) {
  const sales = useSales(period)
  return (
    <Panel
      title={title}
      action={
        <select
          aria-label="Période du graphique"
          value={period}
          onChange={(event) => onPeriodChange(event.target.value as SalesPeriod)}
          className="rounded-lg border border-stone-200 bg-white px-2.5 py-1.5 text-sm"
        >
          <option value="DAY">30 derniers jours</option>
          <option value="MONTH">12 derniers mois</option>
        </select>
      }
    >
      <p className="-mt-2 mb-3 text-sm text-stone-600">En FCFA : paiements en ligne confirmés et espèces reçues à la livraison.</p>
      {sales.isError && <ErrorMessage error={sales.error} />}
      {sales.data ? (
        <SalesChart report={sales.data} dimmed={sales.isPlaceholderData} />
      ) : (
        !sales.isError && <p className="py-24 text-center text-sm text-stone-500">Chargement du graphique…</p>
      )}
    </Panel>
  )
}
