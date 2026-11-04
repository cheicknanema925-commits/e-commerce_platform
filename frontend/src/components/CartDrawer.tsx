import { useEffect, useRef } from 'react'
import { Link } from 'react-router'
import { useCart, useChangeQuantity, useRemoveFromCart } from '../hooks/useCart.ts'
import { formatPrice } from '../lib/format.ts'
import { ErrorMessage } from './ErrorMessage.tsx'
import { Icon } from './Icon.tsx'
import { CartItemThumbnail, QuantityStepper } from './CartLine.tsx'

// Tiroir du panier. <dialog> + showModal() : le navigateur bloque le reste de la page, garde le focus dans le tiroir
// et le ferme avec Échap, sans code d'accessibilité à réécrire.
export function CartDrawer({ open, onClose }: { open: boolean; onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const cart = useCart()
  const changeQuantity = useChangeQuantity()
  const removeFromCart = useRemoveFromCart()
  const error = changeQuantity.error ?? removeFromCart.error

  useEffect(() => {
    const element = dialog.current
    if (open && !element?.open) {
      element?.showModal()
    } else if (!open && element?.open) {
      element.close()
    }
  }, [open])

  const items = cart.data?.items ?? []
  const count = items.reduce((sum, item) => sum + item.quantity, 0)
  return (
    <dialog
      ref={dialog}
      aria-labelledby="titre-panier"
      onClose={onClose}
      // Clic sur le fond sombre (en dehors du tiroir) : fermeture
      onClick={(event) => event.target === event.currentTarget && onClose()}
      className="fixed inset-y-0 right-0 left-auto m-0 h-full max-h-none w-[min(440px,100%)] max-w-none bg-white p-0 text-stone-900 backdrop:bg-brand-900/60"
    >
      <div className="flex h-full flex-col">
        <div className="flex items-center justify-between border-b border-stone-200 px-6 py-5">
          <h2 id="titre-panier" className="text-[22px] font-bold tracking-tight">
            Mon panier
          </h2>
          <button type="button" onClick={onClose} aria-label="Fermer le panier" className="grid size-9 place-items-center rounded-full bg-stone-100 hover:bg-stone-200">
            <Icon name="close" className="size-4" />
          </button>
        </div>

        <div className="flex-1 overflow-auto px-6">
          {cart.isError && <ErrorMessage error={cart.error} />}
          {cart.data && items.length === 0 && (
            <div className="py-14 text-center text-stone-600">
              <p aria-hidden="true" className="text-5xl">
                🧺
              </p>
              <h3 className="mt-3 text-lg font-semibold text-stone-900">Votre panier est vide</h3>
              <p className="mt-1">Ajoutez un produit pour commencer votre commande.</p>
              <Link to="/" onClick={onClose} className="mt-4 inline-block font-semibold text-brand-700 underline underline-offset-3">
                Voir les produits
              </Link>
            </div>
          )}
          <ul>
            {items.map((item) => (
              <li key={item.productId} className="flex gap-3.5 border-b border-stone-200 py-4">
                <CartItemThumbnail item={item} className="size-16" />
                <div className="flex-1 text-sm">
                  <Link to={`/produits/${item.productId}`} onClick={onClose} className="block font-semibold hover:underline">
                    {item.productName}
                  </Link>
                  {formatPrice(item.unitPrice)}
                  <div className="mt-2 flex items-center gap-2.5">
                    <QuantityStepper item={item} onChange={(quantity) => changeQuantity.mutate({ productId: item.productId, quantity })} />
                    <button
                      type="button"
                      onClick={() => removeFromCart.mutate(item.productId)}
                      className="ml-auto text-[13px] font-medium text-bissap underline underline-offset-2"
                    >
                      Supprimer<span className="sr-only"> {item.productName}</span>
                    </button>
                  </div>
                </div>
              </li>
            ))}
          </ul>
          <div aria-live="polite" className="py-3">
            {error && <ErrorMessage error={error} />}
          </div>
        </div>

        {items.length > 0 && (
          <div className="border-t border-stone-200 px-6 pt-5 pb-6">
            <p className="mb-3.5 flex justify-between text-lg font-bold">
              <span>
                Total ({count} article{count > 1 ? 's' : ''})
              </span>
              <span>{formatPrice(cart.data?.total ?? 0)}</span>
            </p>
            <Link
              to="/commande"
              onClick={onClose}
              className="block w-full rounded-full bg-safran py-3.5 text-center font-bold text-stone-900 hover:bg-safran-light"
            >
              Passer la commande
            </Link>
            <p className="mt-2.5 text-center text-[13px] text-stone-600">
              Vous pourrez payer en ligne ou en espèces au livreur.{' '}
              <Link to="/panier" onClick={onClose} className="font-medium text-brand-700 underline">
                Voir le panier
              </Link>
            </p>
          </div>
        )}
      </div>
    </dialog>
  )
}
