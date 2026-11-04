import { Link } from 'react-router'
import type { AdminStats } from '../../api/types.ts'
import { ORDER_VIEWS, orderViewCount } from '../../lib/orderViews.ts'
import { Icon } from '../Icon.tsx'

// Encadré « À traiter » : un lien par étape qui attend l'admin (rien s'il n'y a rien à faire)
export function OrdersToDo({ stats }: { stats: AdminStats }) {
  const toDo = ORDER_VIEWS.filter((view) => view.actionable)
    .map((view) => ({ view, count: orderViewCount(view, stats) }))
    .filter(({ count }) => count > 0)
  if (toDo.length === 0) {
    return null
  }
  return (
    <section id="a-traiter" aria-labelledby="titre-a-traiter" className="mb-4.5 flex gap-4 rounded-lg border border-safran/50 bg-safran-soft px-4.5 py-4">
      <Icon name="bell" className="mt-0.5 size-7" />
      <div>
        <h2 id="titre-a-traiter" className="font-semibold">
          À traiter
        </h2>
        <ul className="mt-2 flex flex-wrap gap-2">
          {toDo.map(({ view, count }) => (
            <li key={view.key}>
              <Link
                to={`/admin/commandes?vue=${view.key}`}
                className="inline-flex items-center gap-2 rounded-full border border-safran/60 bg-white px-3 py-1.5 text-sm font-medium hover:border-stone-900"
              >
                <span className="rounded-full bg-safran px-2 text-xs font-bold">{count}</span>
                {view.label}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
