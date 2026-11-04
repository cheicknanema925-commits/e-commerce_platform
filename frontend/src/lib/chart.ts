import type { SalesPeriod } from '../api/types.ts'

// Graduation « ronde » de l'axe vertical : environ 4 intervalles de 1, 2, 2,5 ou 5 × une puissance de 10
export function niceScale(max: number, intervals = 4): { top: number; step: number } {
  if (max <= 0) {
    return { top: 10_000, step: 2_500 }
  }
  const rawStep = max / intervals
  const magnitude = 10 ** Math.floor(Math.log10(rawStep))
  const step = [1, 2, 2.5, 5, 10].map((m) => m * magnitude).find((s) => s >= rawStep) ?? 10 * magnitude
  return { top: Math.ceil(max / step) * step, step }
}

const fullNumber = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 })
const compactNumber = new Intl.NumberFormat('fr-FR', { notation: 'compact', maximumFractionDigits: 1 })

// Un seul style par axe : en entier sous 10 000 (2 000), compact au-delà (20 k, 1,5 M)
export function formatAxisAmount(value: number, axisTop: number): string {
  return axisTop >= 10_000 ? compactNumber.format(value) : fullNumber.format(value)
}

// « 2026-11-03 » lu comme une date locale (new Date('2026-11-03') serait minuit UTC, donc la veille en Amérique)
function parseDay(isoDay: string): Date {
  const [year, month, day] = isoDay.split('-').map(Number)
  return new Date(year, month - 1, day)
}

const formats = {
  DAY: {
    short: new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }),
    long: new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }),
  },
  MONTH: {
    short: new Intl.DateTimeFormat('fr-FR', { month: 'short' }),
    long: new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }),
  },
}

export function formatPeriod(isoDay: string, period: SalesPeriod, style: 'short' | 'long'): string {
  return formats[period][style].format(parseDay(isoDay))
}
