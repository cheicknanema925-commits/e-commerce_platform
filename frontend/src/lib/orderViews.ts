import type { AdminStats, OrderStatus, PaymentMethod } from '../api/types.ts'

// Onglets de la page Commandes du back-office. « À confirmer » et « Attente de paiement » partagent le statut
// CREATED : on les distingue par le mode de paiement (espèces à confirmer / paiement en ligne pas encore reçu).
export type OrderView = {
  key: string
  label: string
  status?: OrderStatus
  paymentMethod?: PaymentMethod
  // Commandes qui attendent une action de l'admin (badge mis en avant)
  actionable?: boolean
}

export const ORDER_VIEWS: OrderView[] = [
  { key: 'toutes', label: 'Toutes' },
  { key: 'a-confirmer', label: 'À confirmer', status: 'CREATED', paymentMethod: 'CASH_ON_DELIVERY', actionable: true },
  { key: 'payees', label: 'Payées, à préparer', status: 'PAID', actionable: true },
  { key: 'preparation', label: 'En préparation', status: 'PREPARING', actionable: true },
  { key: 'expediees', label: 'Expédiées', status: 'SHIPPED', actionable: true },
  { key: 'livrees', label: 'Livrées', status: 'DELIVERED' },
  { key: 'attente-paiement', label: 'Paiement en attente', status: 'CREATED', paymentMethod: 'ONLINE' },
  { key: 'annulees', label: 'Annulées', status: 'CANCELLED' },
]

export function findOrderView(key: string | null): OrderView {
  return ORDER_VIEWS.find((view) => view.key === key) ?? ORDER_VIEWS[0]
}

export function orderViewCount(view: OrderView, stats: AdminStats): number {
  if (!view.status) {
    return stats.orderCount
  }
  const count = stats.ordersByStatus.find((s) => s.status === view.status)?.count ?? 0
  if (view.status !== 'CREATED') {
    return count
  }
  return view.paymentMethod === 'CASH_ON_DELIVERY' ? stats.awaitingConfirmation : count - stats.awaitingConfirmation
}

// Total des commandes qui attendent l'admin (badge du menu)
export function pendingActionCount(stats: AdminStats): number {
  return ORDER_VIEWS.filter((view) => view.actionable).reduce((sum, view) => sum + orderViewCount(view, stats), 0)
}
