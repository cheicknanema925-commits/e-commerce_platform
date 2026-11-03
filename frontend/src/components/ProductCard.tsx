import { Link } from 'react-router'
import type { Product } from '../api/types.ts'
import { formatPrice } from '../lib/format.ts'

// Un composant reçoit ses données par des « props », comme un constructeur reçoit ses paramètres
export function ProductCard({ product }: { product: Product }) {
  const available = product.stock > 0
  return (
    <article className="relative flex flex-col overflow-hidden rounded-lg border border-stone-200 bg-white shadow-sm transition hover:shadow-md">
      <div
        aria-hidden="true"
        className="flex aspect-[4/3] items-center justify-center bg-brand-50 text-5xl font-bold text-brand-700"
      >
        {product.name.charAt(0)}
      </div>
      <div className="flex flex-1 flex-col gap-1 p-4">
        <p className="text-xs font-medium uppercase tracking-wide text-stone-500">{product.categoryName}</p>
        <h2 className="font-semibold text-stone-900">
          {/* Le lien couvre toute la carte (::after) tout en restant un seul lien pour le clavier */}
          <Link to={`/produits/${product.id}`} className="after:absolute after:inset-0">
            {product.name}
          </Link>
        </h2>
        <p className="mt-auto pt-2 text-lg font-bold text-brand-800">{formatPrice(product.price)}</p>
        <p className={`text-sm ${available ? 'text-stone-600' : 'font-medium text-red-700'}`}>
          {available ? 'En stock' : 'Rupture de stock'}
        </p>
      </div>
    </article>
  )
}
