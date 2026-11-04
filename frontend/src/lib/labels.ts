import type { DeliveryStatus, OrderStatus, PaymentMethod } from '../api/types.ts'

export function orderStatusLabel(status: OrderStatus, paymentMethod: PaymentMethod): string {
  switch (status) {
    case 'CREATED':
      return paymentMethod === 'ONLINE' ? 'En attente de paiement' : 'En attente de confirmation'
    case 'PAID':
      return 'Payée'
    case 'PREPARING':
      return 'En préparation'
    case 'SHIPPED':
      return 'Expédiée'
    case 'DELIVERED':
      return 'Livrée'
    case 'CANCELLED':
      return 'Annulée'
  }
}

// Libellé du bouton de l'admin pour passer au statut suivant
export function adminActionLabel(target: OrderStatus, paymentMethod: PaymentMethod): string {
  switch (target) {
    case 'PREPARING':
      return paymentMethod === 'CASH_ON_DELIVERY' ? 'Confirmer et préparer' : 'Passer en préparation'
    case 'SHIPPED':
      return 'Marquer comme expédiée'
    case 'DELIVERED':
      return paymentMethod === 'CASH_ON_DELIVERY' ? 'Livrée, espèces encaissées' : 'Marquer comme livrée'
    case 'CANCELLED':
      return 'Annuler la commande'
    default:
      return target
  }
}

export const DELIVERY_STEPS: { status: DeliveryStatus; label: string }[] = [
  { status: 'PENDING', label: 'Commande reçue' },
  { status: 'PREPARING', label: 'En préparation' },
  { status: 'SHIPPED', label: 'Expédiée' },
  { status: 'DELIVERED', label: 'Livrée' },
]

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  ONLINE: 'Paiement en ligne (Wave, Orange Money, carte)',
  CASH_ON_DELIVERY: 'Paiement en espèces à la livraison',
}
