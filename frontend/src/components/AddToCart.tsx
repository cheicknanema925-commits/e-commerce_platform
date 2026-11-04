import { useState } from 'react'
import { Link, useLocation } from 'react-router'
import type { Product } from '../api/types.ts'
import { useAuth } from '../auth/useAuth.ts'
import { useAddToCart } from '../hooks/useCart.ts'
import { useToast } from '../lib/toast.ts'
import { ErrorMessage } from './ErrorMessage.tsx'

export function AddToCart({ product }: { product: Product }) {
  const { status } = useAuth()
  const location = useLocation()
  const addToCart = useAddToCart()
  const toast = useToast()
  const [quantity, setQuantity] = useState(1)

  if (product.stock === 0) {
    return null
  }
  if (status !== 'authenticated') {
    return (
      <Link
        to={`/connexion?retour=${encodeURIComponent(location.pathname)}`}
        className="mt-6 inline-block rounded-full bg-safran px-7 py-3.5 font-bold text-stone-900 hover:bg-safran-light"
      >
        Se connecter pour acheter
      </Link>
    )
  }

  return (
    <div className="mt-6">
      <div className="flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="quantite" className="text-sm font-semibold">
            Quantité
          </label>
          <select
            id="quantite"
            value={quantity}
            onChange={(event) => setQuantity(Number(event.target.value))}
            className="rounded-lg border border-stone-200 bg-white px-3 py-3"
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
          onClick={() =>
            addToCart.mutate({ productId: product.id, quantity }, { onSuccess: () => toast(`Ajouté au panier : ${product.name}`) })
          }
          className="flex-1 rounded-full bg-safran px-7 py-3.5 font-bold text-stone-900 hover:bg-safran-light disabled:opacity-60 sm:flex-none"
        >
          {addToCart.isPending ? 'Ajout…' : 'Ajouter au panier'}
        </button>
      </div>
      <div aria-live="polite" className="mt-3">
        {addToCart.isError && <ErrorMessage error={addToCart.error} />}
      </div>
    </div>
  )
}
