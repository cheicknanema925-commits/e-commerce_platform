import type { Delivery } from '../api/types.ts'
import { DELIVERY_STEPS } from '../lib/labels.ts'
import { formatDate } from '../lib/format.ts'

// Suivi de livraison : étapes passées, en cours et à venir (liste ordonnée, lisible au lecteur d'écran)
export function DeliveryTracking({ delivery }: { delivery: Delivery }) {
  const current = DELIVERY_STEPS.findIndex((step) => step.status === delivery.status)
  return (
    <section aria-labelledby="livraison" className="rounded-lg border border-stone-200 bg-white p-6">
      <h2 id="livraison" className="text-lg font-semibold">
        Suivi de livraison
      </h2>
      <ol className="mt-4 grid gap-3 sm:grid-cols-4">
        {DELIVERY_STEPS.map((step, index) => {
          const done = index <= current
          return (
            <li key={step.status} aria-current={index === current ? 'step' : undefined} className="flex items-center gap-2">
              <span
                aria-hidden="true"
                className={`flex size-6 items-center justify-center rounded-full text-xs font-bold ${done ? 'bg-brand-700 text-white' : 'bg-stone-200 text-stone-500'}`}
              >
                {done ? '✓' : index + 1}
              </span>
              <span className={done ? 'font-medium text-stone-900' : 'text-stone-500'}>{step.label}</span>
            </li>
          )
        })}
      </ol>
      <dl className="mt-6 grid gap-2 text-sm text-stone-700 sm:grid-cols-2">
        <div>
          <dt className="font-medium text-stone-900">Adresse</dt>
          <dd>
            {delivery.addressLine}, {delivery.city}
          </dd>
        </div>
        <div>
          <dt className="font-medium text-stone-900">Téléphone</dt>
          <dd>{delivery.phone}</dd>
        </div>
        {delivery.shippedAt && (
          <div>
            <dt className="font-medium text-stone-900">Expédiée le</dt>
            <dd>{formatDate(delivery.shippedAt)}</dd>
          </div>
        )}
        {delivery.deliveredAt && (
          <div>
            <dt className="font-medium text-stone-900">Livrée le</dt>
            <dd>{formatDate(delivery.deliveredAt)}</dd>
          </div>
        )}
      </dl>
    </section>
  )
}
