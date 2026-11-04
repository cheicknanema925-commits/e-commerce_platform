import type { AdminOrder } from '../../api/types.ts'
import { useAssignCourier, useCouriers } from '../../hooks/useAdmin.ts'
import { useToast } from '../../lib/toast.ts'

// Livreur d'une commande. Seuls les livreurs actifs sont proposés ; une commande livrée ou annulée garde le sien
export function CourierSelect({ order }: { order: Pick<AdminOrder, 'id' | 'status' | 'courierId' | 'courierName'> }) {
  const couriers = useCouriers()
  const assign = useAssignCourier()
  const toast = useToast()
  const finished = order.status === 'DELIVERED' || order.status === 'CANCELLED'

  if (finished) {
    return <span className="text-stone-700">{order.courierName ?? '—'}</span>
  }
  const options = couriers.data?.filter((courier) => courier.active || courier.id === order.courierId) ?? []
  return (
    <>
      <label htmlFor={`livreur-${order.id}`} className="sr-only">
        Livreur de la commande n° {order.id}
      </label>
      <select
        id={`livreur-${order.id}`}
        value={order.courierId ?? ''}
        disabled={assign.isPending || !couriers.data}
        onChange={(event) => {
          const courierId = event.target.value ? Number(event.target.value) : null
          assign.mutate(
            { orderId: order.id, courierId },
            {
              onSuccess: () => toast(courierId ? `Commande n° ${order.id} confiée à ${options.find((c) => c.id === courierId)?.name}` : `Commande n° ${order.id} : aucun livreur`),
              onError: (error) => toast(error.message),
            },
          )
        }}
        className="max-w-44 rounded-lg border border-stone-200 bg-white px-2.5 py-1.5 text-[13px] font-medium"
      >
        <option value="">Non assigné</option>
        {options.map((courier) => (
          <option key={courier.id} value={courier.id}>
            {courier.name}
          </option>
        ))}
      </select>
    </>
  )
}
