import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <div className="py-16 text-center">
      <h1 className="text-3xl font-bold tracking-tight">Page introuvable</h1>
      <p className="mt-2 text-stone-600">Ce produit ou cette page n'existe pas (ou plus).</p>
      <Link
        to="/"
        className="mt-6 inline-block rounded-md bg-brand-700 px-4 py-2 font-medium text-white hover:bg-brand-800"
      >
        Voir le catalogue
      </Link>
    </div>
  )
}
