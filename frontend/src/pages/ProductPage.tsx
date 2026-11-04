import { Link, useParams } from 'react-router'
import { ApiError } from '../api/client.ts'
import { AddToCart } from '../components/AddToCart.tsx'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { FavoriteButton } from '../components/FavoriteButton.tsx'
import { Icon } from '../components/Icon.tsx'
import { PriceTag } from '../components/PriceTag.tsx'
import { ProductGallery } from '../components/ProductGallery.tsx'
import { StarRating } from '../components/StarRating.tsx'
import { useCategories, useProduct, useRating, useReviews } from '../hooks/useCatalog.ts'
import { formatDate, formatPrice } from '../lib/format.ts'
import { stockText } from '../lib/stock.ts'
import { NotFoundPage } from './NotFoundPage.tsx'

export function ProductPage() {
  const id = Number(useParams().id)
  const product = useProduct(id)
  const rating = useRating(id)
  const reviews = useReviews(id)
  const categories = useCategories()

  if (!Number.isInteger(id) || (product.error instanceof ApiError && product.error.status === 404)) {
    return <NotFoundPage />
  }
  if (product.isError) {
    return <ErrorMessage error={product.error} />
  }
  if (!product.data) {
    return <p className="text-stone-600">Chargement du produit…</p>
  }

  const { name, categoryId, categoryName, description, price, stock, brand, sku, compareAtPrice, discountPercent } = product.data
  const slug = categories.data?.find((c) => c.id === categoryId)?.slug
  const stockInfo = stockText(stock)
  return (
    <>
      <nav aria-label="Fil d'Ariane" className="text-sm text-stone-600">
        <ol className="flex flex-wrap items-center gap-1.5">
          <li>
            <Link to="/" className="hover:text-brand-700 hover:underline">
              Accueil
            </Link>
          </li>
          <li aria-hidden="true">/</li>
          <li>
            {slug ? (
              <Link to={`/?category=${slug}`} className="hover:text-brand-700 hover:underline">
                {categoryName}
              </Link>
            ) : (
              categoryName
            )}
          </li>
          <li aria-hidden="true">/</li>
          <li aria-current="page" className="font-medium text-stone-900">
            {name}
          </li>
        </ol>
      </nav>

      <article className="mt-5 grid gap-8 rounded-[18px] border border-stone-200 bg-white p-5 sm:p-8 md:grid-cols-2">
        <ProductGallery key={product.data.id} product={product.data} />
        <div>
          <p className="text-sm text-stone-600">
            {brand && (
              <>
                Marque : <b className="font-semibold text-stone-900">{brand}</b> ·{' '}
              </>
            )}
            {categoryName} · Réf. {sku}
          </p>
          <h1 className="mt-1 text-[clamp(28px,3.4vw,40px)] leading-tight font-bold tracking-tight">{name}</h1>
          {rating.data && rating.data.count > 0 && (
            <p className="mt-2 flex items-center gap-2 text-sm text-stone-700">
              <StarRating value={rating.data.average} />
              <a href="#avis" className="hover:underline">
                {rating.data.average.toLocaleString('fr-SN')} ({rating.data.count} avis)
              </a>
            </p>
          )}
          <div className="mt-5 flex flex-wrap items-center gap-3">
            <PriceTag amount={price} size="lg" />
            {compareAtPrice !== null && (
              <>
                <s className="text-lg text-stone-600">
                  <span className="sr-only">Ancien prix : </span>
                  {formatPrice(compareAtPrice)}
                </s>
                <span className="rounded-sm bg-bissap px-2 py-1 text-sm font-bold text-white">−{discountPercent} %</span>
              </>
            )}
          </div>
          <p className={`mt-3 font-medium ${stockInfo.className}`}>{stock > 5 ? `En stock (${stock} disponibles)` : stockInfo.text}</p>
          <AddToCart product={product.data} />
          <div className="mt-3">
            <FavoriteButton product={product.data} large />
          </div>

          <ul className="mt-7 space-y-3 border-t border-stone-200 pt-5 text-sm text-stone-600">
            <li className="flex gap-3">
              <Icon name="truck" className="size-6 text-brand-700" />
              <span>
                <b className="block font-semibold text-stone-900">Livraison à domicile</b>
                Suivez chaque étape : préparation, expédition, livraison.
              </span>
            </li>
            <li className="flex gap-3">
              <Icon name="cash" className="size-6 text-brand-700" />
              <span>
                <b className="block font-semibold text-stone-900">Payez comme vous voulez</b>
                Wave, Orange Money, carte bancaire ou espèces au livreur.
              </span>
            </li>
          </ul>

          {description && (
            <section aria-labelledby="description" className="mt-6">
              <h2 id="description" className="text-lg font-semibold">
                Description
              </h2>
              <p className="mt-2 leading-relaxed whitespace-pre-line text-stone-700">{description}</p>
            </section>
          )}
        </div>
      </article>

      <section aria-labelledby="titre-avis" id="avis" className="mt-14 scroll-mt-40">
        <h2 id="titre-avis" className="text-[clamp(24px,2.8vw,32px)] font-bold tracking-tight">
          Avis des clients
        </h2>
        {reviews.isError && <ErrorMessage error={reviews.error} />}
        {reviews.data?.content.length === 0 && (
          <p className="mt-4 rounded-lg border border-dashed border-stone-300 bg-white p-6 text-stone-600">
            Pas encore d'avis. Les clients qui ont reçu ce produit peuvent le noter.
          </p>
        )}
        <ul className="mt-4 space-y-3">
          {reviews.data?.content.map((review) => (
            <li key={review.id} className="rounded-lg border border-stone-200 bg-white p-5">
              <p className="flex flex-wrap items-center gap-x-3 text-sm text-stone-600">
                <StarRating value={review.rating} />
                <span className="font-semibold text-stone-900">{review.author}</span>
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
