import { Link } from 'react-router'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { ProductCard } from '../components/ProductCard.tsx'
import { useFavorites } from '../hooks/useShop.ts'

export function FavoritesPage() {
  const favorites = useFavorites()

  return (
    <>
      <h1 className="text-[clamp(28px,3.4vw,40px)] font-bold tracking-tight">Mes favoris</h1>
      <p className="mt-1 text-stone-600">Les produits que vous avez aimés, pour les retrouver facilement.</p>
      {favorites.isPending && <p className="mt-6 text-stone-600">Chargement…</p>}
      {favorites.isError && <ErrorMessage error={favorites.error} />}
      {favorites.data?.length === 0 && (
        <div className="mt-6 rounded-[18px] border border-dashed border-stone-300 bg-white p-10 text-center text-stone-600">
          <p>Aucun favori pour l'instant. Touchez le cœur d'un produit pour l'ajouter.</p>
          <Link to="/" className="mt-4 inline-block rounded-full bg-safran px-6 py-3 font-bold text-stone-900 hover:bg-safran-light">
            Voir les produits
          </Link>
        </div>
      )}
      <ul className="mt-6 grid grid-cols-2 gap-2.5 sm:gap-4 md:grid-cols-3 lg:grid-cols-4">
        {favorites.data?.map((product) => (
          <li key={product.id}>
            <ProductCard product={product} />
          </li>
        ))}
      </ul>
    </>
  )
}
