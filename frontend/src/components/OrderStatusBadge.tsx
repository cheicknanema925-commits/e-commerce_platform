import type { OrderStatus, PaymentMethod } from '../../api/types.ts'
import { orderStatusLabel } from '../../lib/labels.ts'

// Le libellé porte le sens ; la couleur aide seulement à repérer d'un coup d'œil (jamais la couleur seule)
const STYLES: Record<OrderStatus, string> = {
  CREATED: 'bg-amber-100 text-amber-900',
  PAID: 'bg-sky-100 text-sky-900',
  PREPARING: 'bg-indigo-100 text-indigo-900',
  SHIPPED: 'bg-violet-100 text-violet-900',
  DELIVERED: 'bg-brand-100 text-brand-900',
  CANCELLED: 'bg-stone-200 text-stone-700',
}

export function OrderStatusBadge({ status, paymentMethod }: { status: OrderStatus; paymentMethod: PaymentMethod }) {
  return (
    <span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold ${STYLES[status]}`}>
      {orderStatusLabel(status, paymentMethod)}
    </span>
  )
}
