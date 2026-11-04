import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <div className="py-16 text-center">
      <p aria-hidden="true" className="text-6xl">🧺</p>
      <h1 className="mt-3 text-3xl font-bold tracking-tight">Page introuvable</h1>
      <p className="mt-2 text-stone-600">Ce produit ou cette page n'existe pas (ou plus).</p>
      <Link
        to="/"
        className="mt-6 inline-block rounded-full bg-safran px-6 py-3 font-bold text-stone-900 hover:bg-safran-light"
      >
        Voir le catalogue
      </Link>
    </div>
  )
}
