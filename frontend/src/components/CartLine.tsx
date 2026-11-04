import type { CartItem } from '../api/types.ts'

// Photo de l'article dans le panier, ou son initiale s'il n'a pas de photo
export function CartItemThumbnail({ item, className }: { item: CartItem; className: string }) {
  if (item.imageUrl) {
    return <img src={item.imageUrl} alt="" className={`shrink-0 rounded-lg bg-stone-100 object-cover ${className}`} />
  }
  return (
    <div aria-hidden="true" className={`grid shrink-0 place-items-center rounded-lg bg-stone-100 text-2xl font-bold text-brand-700 ${className}`}>
      {item.productName.charAt(0)}
    </div>
  )
}

// − quantité + (1 à 99, limite du panier côté API)
export function QuantityStepper({ item, onChange }: { item: CartItem; onChange: (quantity: number) => void }) {
  const buttonClass = 'grid size-7 place-items-center rounded-full border border-stone-200 font-semibold hover:border-brand-700 disabled:opacity-40'
  return (
    <div className="flex items-center gap-2.5">
      <button
        type="button"
        disabled={item.quantity <= 1}
        onClick={() => onChange(item.quantity - 1)}
        aria-label={`Retirer un ${item.productName}`}
        className={buttonClass}
      >
        −
      </button>
      <output aria-label={`Quantité de ${item.productName}`} className="min-w-4 text-center font-semibold">
        {item.quantity}
      </output>
      <button
        type="button"
        disabled={item.quantity >= 99}
        onClick={() => onChange(item.quantity + 1)}
        aria-label={`Ajouter un ${item.productName}`}
        className={buttonClass}
      >
        +
      </button>
    </div>
  )
}
