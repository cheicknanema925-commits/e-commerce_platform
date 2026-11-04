import type { AdminOrder } from '../../api/types.ts'
import { cashState } from '../../lib/admin.ts'

// Mode de paiement + suivi des espèces pour une commande payée à la livraison
export function PaymentCell({ order }: { order: Pick<AdminOrder, 'status' | 'paymentMethod'> }) {
  const cash = cashState(order)
  return (
    <span>
      <span className="inline-flex items-center gap-1.5">
        <i aria-hidden="true" className={`inline-block size-2.5 rounded-xs ${order.paymentMethod === 'ONLINE' ? 'bg-brand-700' : 'bg-bissap'}`} />
        {order.paymentMethod === 'ONLINE' ? 'En ligne' : 'Espèces à la livraison'}
      </span>
      {cash === 'due' && <span className="block text-xs font-semibold text-bissap">Espèces à encaisser</span>}
      {cash === 'collected' && <span className="block text-xs font-semibold text-kola">Espèces encaissées</span>}
    </span>
  )
}
