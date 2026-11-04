import type { Product } from '../api/types.ts'

// Photo principale du produit, ou son initiale si aucune photo n'a encore été ajoutée.
// alt vide : le nom du produit est déjà écrit à côté, un lecteur d'écran le lirait deux fois.
export function ProductThumbnail({ product, className }: { product: Pick<Product, 'name' | 'images'>; className: string }) {
  const image = product.images[0]
  if (image) {
    return <img src={image.url} alt="" loading="lazy" className={`object-cover ${className}`} />
  }
  return (
    <div aria-hidden="true" className={`flex items-center justify-center bg-brand-50 font-bold text-brand-700 ${className}`}>
      {product.name.charAt(0)}
    </div>
  )
}
