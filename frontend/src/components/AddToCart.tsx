import { useState } from 'react'
import { Link, useLocation } from 'react-router'
import type { Product } from '../api/types.ts'
import { useAuth } from '../auth/useAuth.ts'
import { useAddToCart } from '../hooks/useCart.ts'
import { ErrorMessage } from './ErrorMessage.tsx'

export function AddToCart({ product }: { product: Product }) {
  const { status } = useAuth()
  const location = useLocation()
  const addToCart = useAddToCart()
  const [quantity, setQuantity] = useState(1)

  if (product.stock === 0) {
    return null
  }
  if (status !== 'authenticated') {
    return (
      <Link
        to={`/connexion?retour=${encodeURIComponent(location.pathname)}`}
        className="mt-6 inline-block rounded-md bg-brand-700 px-6 py-3 font-medium text-white hover:bg-brand-800"
      >
        Se connecter pour acheter
      </Link>
    )
  }

  return (
    <div className="mt-6">
      <div className="flex items-end gap-3">
        <div className="flex flex-col gap-1">
          <label htmlFor="quantite" className="text-sm font-medium text-stone-700">
            Quantité
          </label>
          <select
            id="quantite"
            value={quantity}
            onChange={(event) => setQuantity(Number(event.target.value))}
            className="rounded-md border border-stone-300 bg-white px-3 py-2"
          >
            {Array.from({ length: Math.min(product.stock, 10) }, (_, i) => i + 1).map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </div>
        <button
          type="button"
          disabled={addToCart.isPending}
          onClick={() => addToCart.mutate({ productId: product.id, quantity })}
          className="rounded-md bg-brand-700 px-6 py-2.5 font-medium text-white hover:bg-brand-800 disabled:opacity-60"
        >
          {addToCart.isPending ? 'Ajout…' : 'Ajouter au panier'}
        </button>
      </div>
      <div aria-live="polite" className="mt-3">
        {addToCart.isSuccess && (
          <p className="text-sm text-brand-800">
            Ajouté au panier.{' '}
            <Link to="/panier" className="font-medium underline">
              Voir le panier
            </Link>
          </p>
        )}
        {addToCart.isError && <ErrorMessage error={addToCart.error} />}
      </div>
    </div>
  )
}
