import type { ReactNode } from 'react'
import { formatNumber } from '../../lib/format.ts'

// Briques communes du back-office, reprises de la maquette (titre de page, bande d'indicateurs, panneaux, tableaux)

export const btnPrimary =
  'inline-flex items-center gap-2 rounded-full bg-safran px-4.5 py-2.5 text-sm font-bold text-stone-900 hover:bg-safran-light disabled:opacity-60'
export const btnGhost =
  'inline-flex items-center gap-2 rounded-full border border-stone-200 bg-white px-4.5 py-2.5 text-sm font-semibold hover:border-brand-700 hover:text-brand-700'
export const btnMini = 'rounded-lg border border-stone-200 bg-white px-2.5 py-1.5 text-[13px] font-medium hover:border-brand-700 hover:text-brand-700 disabled:opacity-50'
export const btnMiniDanger = 'rounded-lg border border-stone-200 bg-white px-2.5 py-1.5 text-[13px] font-medium hover:border-bissap hover:text-bissap disabled:opacity-50'
export const filterInput = 'rounded-full border border-stone-200 bg-white px-4 py-2.5 text-sm outline-none focus:border-brand-700'

export function PageHeader({ title, description, actions }: { title: string; description?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="mb-5.5 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-[clamp(26px,3vw,34px)] leading-tight font-bold tracking-tight">{title}</h1>
        {description && <p className="mt-1.5 text-[15px] text-stone-600">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  )
}

// Bande d'indicateurs : gap-px sur fond gris = traits de séparation qui s'adaptent au nombre de colonnes
export function KpiStrip({ children }: { children: ReactNode }) {
  return <dl className="mb-5 grid grid-cols-2 gap-px overflow-hidden rounded-lg border border-stone-200 bg-stone-200 xl:grid-cols-4">{children}</dl>
}

export function Kpi({ label, value, unit, detail, accent = false }: { label: string; value: string; unit?: string; detail?: ReactNode; accent?: boolean }) {
  return (
    <div className={`min-w-0 px-4 py-3 sm:px-5.5 sm:py-4.5 ${accent ? 'bg-safran-soft' : 'bg-white'}`}>
      <dt className="text-sm text-stone-600">{label}</dt>
      <dd className="mt-1.5 mb-1 text-[21px] leading-tight font-bold tracking-tight sm:text-[30px]">
        {value}
        {unit && <small className="ml-1 text-xs font-medium text-stone-600 sm:text-sm">{unit}</small>}
      </dd>
      {detail && <dd className="text-[13px] text-stone-600">{detail}</dd>}
    </div>
  )
}

// Montant en indicateur : « 4 850 000 » + « FCFA » en petit
export function AmountKpi(props: { label: string; amount: number; detail?: ReactNode; accent?: boolean }) {
  return <Kpi label={props.label} value={formatNumber(props.amount)} unit="FCFA" detail={props.detail} accent={props.accent} />
}

export function Panel({ title, action, children, className = '' }: { title: string; action?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={`min-w-0 rounded-lg border border-stone-200 bg-white px-5.5 py-5 ${className}`}>
      <div className="mb-4 flex items-center justify-between gap-2.5">
        <h2 className="text-lg font-semibold tracking-tight">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  )
}

export function TableCard({ title, count, action, children }: { title: string; count?: number; action?: ReactNode; children: ReactNode }) {
  return (
    <section className="mt-4.5 overflow-hidden rounded-lg border border-stone-200 bg-white">
      <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
        <h2 className="text-lg font-semibold">
          {title} {count !== undefined && <span className="font-normal text-stone-600">({count})</span>}
        </h2>
        {action}
      </div>
      {/* relative : les libellés sr-only (positionnés en absolu) restent dans la zone qui défile, sinon ils élargissent la page */}
      <div className="relative overflow-x-auto">{children}</div>
    </section>
  )
}

export const th = 'bg-stone-100 px-5 py-2.5 text-left text-[13px] font-semibold whitespace-nowrap text-stone-600'
export const td = 'border-t border-stone-200 px-5 py-3 align-middle whitespace-nowrap'

// Barres horizontales classées (une seule teinte : la longueur porte la valeur, le texte la donne en clair)
export function BarList({ rows }: { rows: { label: string; value: number; display: string }[] }) {
  const max = Math.max(1, ...rows.map((row) => row.value))
  return (
    <ul className="space-y-3.5">
      {rows.map((row) => (
        <li key={row.label}>
          <div className="flex justify-between gap-3 text-sm">
            <span>{row.label}</span>
            <b className="font-semibold tabular-nums">{row.display}</b>
          </div>
          <div aria-hidden="true" className="mt-1.5 h-2 overflow-hidden rounded-full bg-stone-100">
            <div className="h-full rounded-full bg-brand-500" style={{ width: `${(row.value / max) * 100}%` }} />
          </div>
        </li>
      ))}
    </ul>
  )
}

export function Meter({ label, value, ratio, tone }: { label: string; value: string; ratio: number; tone: 'kola' | 'bissap' | 'brand' }) {
  const color = { kola: 'bg-kola', bissap: 'bg-bissap', brand: 'bg-brand-500' }[tone]
  return (
    <div className="mt-3 first:mt-0">
      <div className="flex justify-between text-sm">
        <span>{label}</span>
        <b className="font-semibold tabular-nums">{value}</b>
      </div>
      <div aria-hidden="true" className="mt-1.5 h-2 overflow-hidden rounded-full bg-stone-100">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${Math.min(100, Math.max(0, ratio * 100))}%` }} />
      </div>
    </div>
  )
}
