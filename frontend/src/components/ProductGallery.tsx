import { useState } from 'react'
import type { Product } from '../api/types.ts'
import { ProductThumbnail } from './ProductThumbnail.tsx'

// Grande photo + vignettes pour changer de photo (boutons : utilisables au clavier)
export function ProductGallery({ product }: { product: Product }) {
  const [selected, setSelected] = useState(0)
  const image = product.images[selected] ?? product.images[0]

  if (!image) {
    return <ProductThumbnail product={product} className="aspect-square w-full rounded-lg text-[120px]" />
  }
  return (
    <div>
      <img src={image.url} alt={product.name} className="aspect-square w-full rounded-lg bg-stone-100 object-contain" />
      {product.images.length > 1 && (
        <ul className="mt-3 flex flex-wrap gap-2">
          {product.images.map((thumbnail, index) => (
            <li key={thumbnail.id}>
              <button
                type="button"
                aria-pressed={index === selected}
                aria-label={`Afficher la photo ${index + 1}`}
                onClick={() => setSelected(index)}
                className={`overflow-hidden rounded-lg border-2 ${index === selected ? 'border-safran' : 'border-transparent hover:border-stone-300'}`}
              >
                <img src={thumbnail.url} alt="" loading="lazy" className="size-16 object-cover" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
