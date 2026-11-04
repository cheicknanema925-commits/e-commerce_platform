import { Link, useParams } from 'react-router'
import { ApiError } from '../api/client.ts'
import { AddToCart } from '../components/AddToCart.tsx'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { ProductGallery } from '../components/ProductGallery.tsx'
import { StarRating } from '../components/StarRating.tsx'
import { useProduct, useRating, useReviews } from '../hooks/useCatalog.ts'
import { formatDate, formatPrice } from '../lib/format.ts'
import { NotFoundPage } from './NotFoundPage.tsx'

export function ProductPage() {
  const id = Number(useParams().id)
  const product = useProduct(id)
  const rating = useRating(id)
  const reviews = useReviews(id)

  if (!Number.isInteger(id) || (product.error instanceof ApiError && product.error.status === 404)) {
    return <NotFoundPage />
  }
  if (product.isError) {
    return <ErrorMessage error={product.error} />
  }
  if (!product.data) {
    return <p className="text-stone-600">Chargement du produit…</p>
  }

  const { name, categoryName, description, price, stock } = product.data
  return (
    <>
      <Link to="/" className="text-sm font-medium text-brand-700 hover:underline">
        ← Retour au catalogue
      </Link>

      <article className="mt-4 grid gap-8 md:grid-cols-2">
        <ProductGallery key={product.data.id} product={product.data} />
        <div>
          <p className="text-sm font-medium uppercase tracking-wide text-stone-500">{categoryName}</p>
          <h1 className="mt-1 text-3xl font-bold tracking-tight">{name}</h1>
          {rating.data && rating.data.count > 0 && (
            <p className="mt-2 flex items-center gap-2 text-sm text-stone-700">
              <StarRating value={rating.data.average} />
              <span>
                {rating.data.average.toLocaleString('fr-SN')} ({rating.data.count} avis)
              </span>
            </p>
          )}
          <p className="mt-4 text-3xl font-bold text-brand-800">{formatPrice(price)}</p>
          <p className={`mt-1 ${stock > 0 ? 'text-stone-700' : 'font-medium text-red-700'}`}>
            {stock > 0 ? `En stock (${stock} disponible${stock > 1 ? 's' : ''})` : 'Rupture de stock'}
          </p>
          <AddToCart product={product.data} />
          {description && <p className="mt-6 leading-relaxed text-stone-700">{description}</p>}
        </div>
      </article>

      <section aria-labelledby="avis" className="mt-12">
        <h2 id="avis" className="text-2xl font-bold tracking-tight">
          Avis des clients
        </h2>
        {reviews.isError && <ErrorMessage error={reviews.error} />}
        {reviews.data?.content.length === 0 && (
          <p className="mt-4 text-stone-600">Pas encore d'avis. Les clients qui ont reçu ce produit peuvent le noter.</p>
        )}
        <ul className="mt-4 space-y-4">
          {reviews.data?.content.map((review) => (
            <li key={review.id} className="rounded-lg border border-stone-200 bg-white p-4">
              <p className="flex flex-wrap items-center gap-x-3 text-sm text-stone-600">
                <StarRating value={review.rating} />
                <span className="font-medium text-stone-900">{review.author}</span>
                <time dateTime={review.createdAt}>{formatDate(review.createdAt)}</time>
              </p>
              {/* Texte écrit par un client : React l'affiche comme du texte, jamais comme du HTML (pas de XSS) */}
              {review.comment && <p className="mt-2 text-stone-800">{review.comment}</p>}
            </li>
          ))}
        </ul>
      </section>
    </>
  )
}
