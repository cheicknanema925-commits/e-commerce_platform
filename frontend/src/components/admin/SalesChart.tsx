import { type KeyboardEvent, type RefObject, useEffect, useRef, useState } from 'react'
import type { SalesReport } from '../../api/types.ts'
import { formatAxisAmount, formatPeriod, niceScale } from '../../lib/chart.ts'
import { formatPrice } from '../../lib/format.ts'

// Hauteur totale, axe des dates compris (jamais de libellés coupés en bas)
const HEIGHT = 260
const MARGIN = { top: 24, right: 8, bottom: 28, left: 44 }

// Largeur réelle du conteneur : le SVG est dessiné à la bonne taille (texte lisible sur mobile comme sur écran large)
function useWidth(ref: RefObject<HTMLDivElement | null>): number {
  const [width, setWidth] = useState(0)
  useEffect(() => {
    const element = ref.current
    if (!element) {
      return
    }
    const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width))
    observer.observe(element)
    return () => observer.disconnect()
  }, [ref])
  return width
}

// Colonne aux coins supérieurs arrondis (4 px), carrée sur la ligne de base
function columnPath(x: number, y: number, width: number, height: number): string {
  const r = Math.min(4, width / 2, height)
  return `M${x},${y + height}V${y + r}Q${x},${y} ${x + r},${y}H${x + width - r}Q${x + width},${y} ${x + width},${y + r}V${y + height}Z`
}

// Montant encaissé par jour ou par mois : une seule série, donc une seule couleur et pas de légende (le titre suffit)
export function SalesChart({ report, dimmed }: { report: SalesReport; dimmed: boolean }) {
  const containerRef = useRef<HTMLDivElement>(null)
  const width = useWidth(containerRef)
  const [active, setActive] = useState<number | null>(null)

  const { points, period } = report
  const max = Math.max(0, ...points.map((p) => p.collected))
  const { top, step } = niceScale(max)
  const plotWidth = Math.max(0, width - MARGIN.left - MARGIN.right)
  const plotHeight = HEIGHT - MARGIN.top - MARGIN.bottom
  const band = points.length > 0 ? plotWidth / points.length : 0
  // Colonne fine (24 px max), jamais collée à sa voisine
  const barWidth = Math.max(2, Math.min(24, band * 0.7))
  const y = (value: number) => MARGIN.top + plotHeight - (value / top) * plotHeight
  const ticks = Array.from({ length: Math.round(top / step) + 1 }, (_, i) => i * step)
  // Une date sur k pour que les libellés ne se chevauchent pas ; la dernière (aujourd'hui) est toujours affichée
  const labelEvery = Math.max(1, Math.ceil(points.length / Math.max(1, Math.floor(plotWidth / 64))))
  // Seule la plus haute colonne porte sa valeur : les autres sont dans l'info-bulle et le tableau
  const highest = max > 0 ? points.findLastIndex((p) => p.collected === max) : -1

  const unit = period === 'DAY' ? 'jour' : 'mois'
  const activePoint = active !== null ? points[active] : null
  const describe = (index: number) => {
    const point = points[index]
    const orders = `${point.orders} commande${point.orders > 1 ? 's' : ''}`
    return `${formatPeriod(point.start, period, 'long')} : ${formatPrice(point.collected)} encaissés, ${orders}`
  }

  function onKeyDown(event: KeyboardEvent<SVGSVGElement>) {
    const last = points.length - 1
    const current = active ?? last
    const next = { ArrowLeft: current - 1, ArrowRight: current + 1, Home: 0, End: last }[event.key]
    if (next !== undefined) {
      event.preventDefault()
      setActive(Math.min(last, Math.max(0, next)))
    }
  }

  return (
    <div>
      <div ref={containerRef} className={`relative transition-opacity ${dimmed ? 'opacity-50' : ''}`}>
        {width > 0 && (
          <svg
            width={width}
            height={HEIGHT}
            tabIndex={0}
            role="group"
            aria-label={`Montant encaissé par ${unit}. Flèches gauche et droite pour parcourir les ${unit === 'jour' ? 'jours' : 'mois'}.`}
            onKeyDown={onKeyDown}
            onFocus={() => setActive((current) => current ?? points.length - 1)}
            onBlur={() => setActive(null)}
            onPointerLeave={() => setActive(null)}
            className="block rounded-md"
          >
            <g aria-hidden="true">
              {ticks.map((tick) => (
                <g key={tick}>
                  <line x1={MARGIN.left} x2={width - MARGIN.right} y1={y(tick)} y2={y(tick)} className="stroke-stone-200" strokeWidth={1} />
                  <text x={MARGIN.left - 8} y={y(tick)} dy="0.32em" textAnchor="end" className="fill-stone-500 text-[11px] tabular-nums">
                    {formatAxisAmount(tick, top)}
                  </text>
                </g>
              ))}

              {points.map((point, i) => {
                const x = MARGIN.left + i * band + (band - barWidth) / 2
                const height = plotHeight - (y(point.collected) - MARGIN.top)
                const showLabel = (points.length - 1 - i) % labelEvery === 0
                return (
                  <g key={point.start}>
                    {height > 0 && (
                      <path
                        d={columnPath(x, y(point.collected), barWidth, height)}
                        className={active === i ? 'fill-brand-800' : 'fill-brand-600'}
                      />
                    )}
                    {i === highest && (
                      <text x={x + barWidth / 2} y={y(point.collected) - 6} textAnchor="middle" className="fill-stone-700 text-[11px] font-semibold">
                        {formatAxisAmount(point.collected, top)}
                      </text>
                    )}
                    {showLabel && (
                      <text x={x + barWidth / 2} y={HEIGHT - 8} textAnchor="middle" className="fill-stone-500 text-[11px]">
                        {formatPeriod(point.start, period, 'short')}
                      </text>
                    )}
                    {/* Zone de survol : toute la hauteur et toute la largeur de la tranche, plus grande que la colonne */}
                    <rect
                      x={MARGIN.left + i * band}
                      y={MARGIN.top}
                      width={band}
                      height={plotHeight}
                      fill="transparent"
                      onPointerEnter={() => setActive(i)}
                    />
                  </g>
                )
              })}
            </g>
          </svg>
        )}

        {max === 0 && width > 0 && (
          <p className="pointer-events-none absolute inset-x-0 top-1/3 text-center text-sm text-stone-500">
            Aucun encaissement sur cette période
          </p>
        )}

        {activePoint && active !== null && (
          <div
            aria-hidden="true"
            className="pointer-events-none absolute z-10 min-w-36 -translate-x-1/2 -translate-y-full rounded-md border border-stone-200 bg-white px-3 py-2 text-sm shadow-lg"
            style={{
              left: Math.min(Math.max(MARGIN.left + band * (active + 0.5), 80), width - 80),
              top: Math.max(y(activePoint.collected) - 10, 64),
            }}
          >
            <p className="font-semibold text-stone-900">{formatPrice(activePoint.collected)}</p>
            <p className="text-stone-600">{formatPeriod(activePoint.start, period, 'long')}</p>
            <p className="text-stone-600">
              {activePoint.orders} commande{activePoint.orders > 1 ? 's' : ''}
            </p>
          </div>
        )}
      </div>

      {/* Annonce la colonne choisie au clavier aux lecteurs d'écran */}
      <p aria-live="polite" className="sr-only">
        {active !== null ? describe(active) : ''}
      </p>

      <details className="mt-2 text-sm">
        <summary className="cursor-pointer font-medium text-brand-700">Voir les données sous forme de tableau</summary>
        <table className="mt-2 w-full text-left">
          <thead className="text-stone-600">
            <tr>
              <th scope="col" className="py-1 font-medium">
                {period === 'DAY' ? 'Jour' : 'Mois'}
              </th>
              <th scope="col" className="py-1 text-right font-medium">
                Encaissé
              </th>
              <th scope="col" className="py-1 text-right font-medium">
                Commandes
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100 tabular-nums">
            {points.map((point) => (
              <tr key={point.start}>
                <td className="py-1">{formatPeriod(point.start, period, 'long')}</td>
                <td className="py-1 text-right">{formatPrice(point.collected)}</td>
                <td className="py-1 text-right">{point.orders}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </div>
  )
}
