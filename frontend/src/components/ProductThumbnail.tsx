import type { Product } from '../api/types.ts'
import { useCategories } from '../hooks/useCatalog.ts'
import { categoryVisual } from '../lib/categories.ts'

// Photo principale du produit ; sans photo, l'emoji et la couleur de son rayon (comme la maquette).
// alt vide : le nom du produit est déjà écrit à côté, un lecteur d'écran le lirait deux fois.
export function ProductThumbnail({ product, className }: { product: Pick<Product, 'images' | 'categoryId'>; className: string }) {
  const categories = useCategories()
  const image = product.images[0]
  if (image) {
    return <img src={image.url} alt="" loading="lazy" className={`bg-white object-cover ${className}`} />
  }
  const visual = categoryVisual(categories.data?.find((c) => c.id === product.categoryId)?.slug)
  return (
    <div aria-hidden="true" className={`grid place-items-center ${visual.tint} ${className}`}>
      {visual.emoji}
    </div>
  )
}
