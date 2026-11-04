import type { OrderStatus, PaymentMethod } from '../api/types.ts'
import { orderStatusLabel } from '../lib/labels.ts'

// Le libellé porte le sens ; la couleur aide seulement à repérer d'un coup d'œil (jamais la couleur seule)
const STYLES: Record<OrderStatus, string> = {
  CREATED: 'bg-safran-soft text-safran-ink',
  PAID: 'bg-brand-100 text-brand-700',
  PREPARING: 'bg-brand-100 text-brand-700',
  SHIPPED: 'bg-safran-soft text-safran-ink',
  DELIVERED: 'bg-kola-soft text-kola',
  CANCELLED: 'bg-bissap-soft text-bissap',
}

export function OrderStatusBadge({ status, paymentMethod }: { status: OrderStatus; paymentMethod: PaymentMethod }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[13px] font-semibold whitespace-nowrap before:size-[7px] before:rounded-full before:bg-current ${STYLES[status]}`}
    >
      {orderStatusLabel(status, paymentMethod)}
    </span>
  )
}
