import { Link, useNavigate } from 'react-router'
import type { Product } from '../api/types.ts'
import { useAuth } from '../auth/useAuth.ts'
import { useAddToCart, useCart } from '../hooks/useCart.ts'
import { formatPrice } from '../lib/format.ts'
import { stockText } from '../lib/stock.ts'
import { useToast } from '../lib/toast.ts'
import { FavoriteButton } from './FavoriteButton.tsx'
import { PriceTag } from './PriceTag.tsx'
import { ProductThumbnail } from './ProductThumbnail.tsx'

// Un composant reçoit ses données par des « props », comme un constructeur reçoit ses paramètres
export function ProductCard({ product }: { product: Product }) {
  const { status } = useAuth()
  const cart = useCart()
  const addToCart = useAddToCart()
  const toast = useToast()
  const navigate = useNavigate()
  const inCart = cart.data?.items.some((item) => item.productId === product.id) ?? false
  const stock = stockText(product.stock)

  function onAdd() {
    if (status !== 'authenticated') {
      navigate(`/connexion?retour=${encodeURIComponent(`/produits/${product.id}`)}`)
      return
    }
    addToCart.mutate(
      { productId: product.id, quantity: 1 },
      {
        onSuccess: () => toast(`Ajouté au panier : ${product.name}`),
        onError: (error) => toast(error.message),
      },
    )
  }

  return (
    <article className="relative flex h-full flex-col overflow-hidden rounded-lg border border-stone-200 bg-white transition hover:border-stone-400">
      <div className="relative">
        <ProductThumbnail product={product} className="aspect-[4/3] w-full text-7xl" />
        {product.stock === 0 ? (
          <span className="absolute top-3 left-3 rounded-sm bg-stone-900 px-2 py-1 text-[13px] font-bold text-white">Rupture</span>
        ) : (
          product.discountPercent !== null && (
            <span className="absolute top-3 left-3 rounded-sm bg-bissap px-2 py-1 text-[13px] font-bold text-white">−{product.discountPercent} %</span>
          )
        )}
        <div className="absolute top-2.5 right-2.5">
          <FavoriteButton product={product} />
        </div>
      </div>
      <div className="flex flex-1 flex-col px-4 pt-3.5 pb-4">
        <p className="text-[13px] text-stone-600">{product.brand ? `${product.brand} · ${product.categoryName}` : product.categoryName}</p>
        <h3 className="mt-0.5 mb-3 min-h-[42px] leading-snug font-semibold">
          {/* Le lien couvre toute la carte (::after) tout en restant un seul lien pour le clavier */}
          <Link to={`/produits/${product.id}`} className="after:absolute after:inset-0">
            {product.name}
          </Link>
        </h3>
        <div className="mt-auto flex flex-wrap items-center gap-2.5">
          <PriceTag amount={product.price} />
          {product.compareAtPrice !== null && (
            <s className="text-sm text-stone-600">
              <span className="sr-only">Ancien prix : </span>
              {formatPrice(product.compareAtPrice)}
            </s>
          )}
        </div>
        <p className={`mt-2.5 text-[13px] font-medium ${stock.className}`}>{stock.text}</p>
        {product.stock > 0 && (
          <button
            type="button"
            onClick={onAdd}
            disabled={addToCart.isPending}
            className={`relative z-10 mt-3 w-full rounded-full border-2 py-2 text-sm font-semibold transition disabled:opacity-60 ${
              inCart ? 'border-kola bg-kola text-white' : 'border-brand-700 text-brand-700 hover:bg-brand-700 hover:text-white'
            }`}
          >
            {addToCart.isPending ? 'Ajout…' : inCart ? 'Ajouté au panier' : 'Ajouter au panier'}
            <span className="sr-only"> : {product.name}</span>
          </button>
        )}
      </div>
    </article>
  )
}
