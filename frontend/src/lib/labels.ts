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
