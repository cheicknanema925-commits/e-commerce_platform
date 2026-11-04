import type { Delivery } from '../api/types.ts'
import { formatDate } from '../lib/format.ts'
import { DELIVERY_STEPS } from '../lib/labels.ts'
import { osmLink } from '../lib/location.ts'
import { Icon, type IconName } from './Icon.tsx'

const STEP_ICONS: IconName[] = ['check', 'box', 'truck', 'store']

// Suivi de livraison : frise des étapes passées, en cours et à venir (liste ordonnée, lisible au lecteur d'écran)
export function DeliveryTracking({ delivery }: { delivery: Delivery }) {
  const current = DELIVERY_STEPS.findIndex((step) => step.status === delivery.status)
  const dates: Partial<Record<string, string | null>> = { SHIPPED: delivery.shippedAt, DELIVERED: delivery.deliveredAt }
  return (
    <section aria-labelledby="livraison" className="rounded-lg border border-stone-200 bg-white p-6">
      <h2 id="livraison" className="text-lg font-semibold">
        Suivi de livraison
      </h2>
      <ol className="mt-6 grid gap-4 sm:grid-cols-4 sm:gap-0">
        {DELIVERY_STEPS.map((step, index) => {
          const done = index < current || delivery.status === 'DELIVERED'
          const active = index === current && !done
          const date = dates[step.status]
          return (
            <li
              key={step.status}
              aria-current={index === current ? 'step' : undefined}
              className="relative flex items-center gap-3 sm:flex-col sm:text-center"
            >
              {/* Trait qui relie les étapes (sur grand écran) : vert jusqu'à l'étape atteinte */}
              {index > 0 && (
                <span aria-hidden="true" className={`absolute top-5 right-1/2 hidden h-1 w-full -translate-y-1/2 sm:block ${index <= current ? 'bg-kola' : 'bg-stone-200'}`} />
              )}
              <span
                aria-hidden="true"
                className={`relative grid size-10 shrink-0 place-items-center rounded-full ring-4 ring-white ${
                  done ? 'bg-kola text-white' : active ? 'bg-brand-700 text-white' : 'bg-stone-100 text-stone-400'
                }`}
              >
                <Icon name={done ? 'check' : STEP_ICONS[index]} className="size-5" />
              </span>
              <span className="sm:mt-2">
                <span className={`block text-sm ${done || active ? 'font-semibold text-stone-900' : 'text-stone-500'}`}>{step.label}</span>
                <span className="block text-xs text-stone-600">
                  {done ? (date ? formatDate(date) : 'Terminé') : active ? 'En cours' : 'À venir'}
                </span>
              </span>
            </li>
          )
        })}
      </ol>
      <dl className="mt-6 grid gap-3 border-t border-stone-200 pt-5 text-sm text-stone-700 sm:grid-cols-3">
        <div>
          <dt className="font-semibold text-stone-900">Adresse</dt>
          <dd>
            {delivery.addressLine}, {delivery.city}
          </dd>
        </div>
        <div>
          <dt className="font-semibold text-stone-900">Téléphone</dt>
          <dd>{delivery.phone}</dd>
        </div>
        <div>
          <dt className="font-semibold text-stone-900">Position GPS</dt>
          <dd>
            {delivery.latitude !== null && delivery.longitude !== null ? (
              <a
                href={osmLink({ latitude: delivery.latitude, longitude: delivery.longitude })}
                target="_blank"
                rel="noopener noreferrer"
                className="font-medium text-brand-700 underline"
              >
                Voir sur la carte<span className="sr-only"> (nouvel onglet)</span>
              </a>
            ) : (
              'Non fournie'
            )}
          </dd>
        </div>
      </dl>
    </section>
  )
}
