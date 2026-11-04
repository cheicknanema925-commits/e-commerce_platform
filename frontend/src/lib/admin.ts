import type { AdminOrder } from '../api/types.ts'

// Initiales pour l'avatar : « Mariama Sow » → « MS »
export function initials(fullName: string): string {
  return fullName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word.charAt(0).toUpperCase())
    .join('')
}

// Part en pourcentage arrondi (« 38 % »), 0 % si le total est nul
export function percent(part: number, total: number): string {
  return `${total > 0 ? Math.round((part / total) * 100) : 0} %`
}

// Espèces d'une commande payée à la livraison : encore chez le livreur, encaissées (à la livraison), ou sans objet
export type CashState = 'due' | 'collected' | 'cancelled'

export function cashState(order: Pick<AdminOrder, 'status' | 'paymentMethod'>): CashState | null {
  if (order.paymentMethod !== 'CASH_ON_DELIVERY') {
    return null
  }
  if (order.status === 'CANCELLED') {
    return 'cancelled'
  }
  return order.status === 'DELIVERED' ? 'collected' : 'due'
}
