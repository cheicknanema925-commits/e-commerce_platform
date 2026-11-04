import { Link } from 'react-router'
import { CartItemThumbnail, QuantityStepper } from '../components/CartLine.tsx'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { useCart, useChangeQuantity, useRemoveFromCart } from '../hooks/useCart.ts'
import { formatPrice } from '../lib/format.ts'

export function CartPage() {
  const cart = useCart()
  const changeQuantity = useChangeQuantity()
  const removeFromCart = useRemoveFromCart()
  const mutationError = changeQuantity.error ?? removeFromCart.error

  if (cart.isPending) {
    return <p className="text-stone-600">Chargement du panier…</p>
  }
  if (cart.isError) {
    return <ErrorMessage error={cart.error} />
  }

  const count = cart.data.items.reduce((sum, item) => sum + item.quantity, 0)
  return (
    <>
      <h1 className="text-[clamp(28px,3.4vw,40px)] font-bold tracking-tight">Mon panier</h1>
      {cart.data.items.length === 0 ? (
        <div className="mt-6 rounded-[18px] border border-dashed border-stone-300 bg-white p-12 text-center text-stone-600">
          <p aria-hidden="true" className="text-5xl">
            🧺
          </p>
          <h2 className="mt-3 text-lg font-semibold text-stone-900">Votre panier est vide</h2>
          <p className="mt-1">Ajoutez un produit pour commencer votre commande.</p>
          <Link to="/" className="mt-5 inline-block rounded-full bg-safran px-6 py-3 font-bold text-stone-900 hover:bg-safran-light">
            Voir les produits
          </Link>
        </div>
      ) : (
        <div className="mt-6 grid gap-6 lg:grid-cols-3">
          <ul className="rounded-lg border border-stone-200 bg-white px-5 lg:col-span-2">
            {cart.data.items.map((item) => (
              <li key={item.productId} className="flex gap-4 border-b border-stone-200 py-4 last:border-b-0">
                <CartItemThumbnail item={item} className="size-20" />
                <div className="flex flex-1 flex-wrap items-start justify-between gap-3">
                  <div>
                    <Link to={`/produits/${item.productId}`} className="font-semibold hover:underline">
                      {item.productName}
                    </Link>
                    <p className="text-sm text-stone-600">{formatPrice(item.unitPrice)} l'unité</p>
                    <div className="mt-2.5 flex items-center gap-4">
                      <QuantityStepper item={item} onChange={(quantity) => changeQuantity.mutate({ productId: item.productId, quantity })} />
                      <button
                        type="button"
                        onClick={() => removeFromCart.mutate(item.productId)}
                        className="text-[13px] font-medium text-bissap underline underline-offset-2"
                      >
                        Supprimer<span className="sr-only"> {item.productName}</span>
                      </button>
                    </div>
                  </div>
                  <p className="font-bold">{formatPrice(item.subtotal)}</p>
                </div>
              </li>
            ))}
          </ul>

          <aside className="h-fit rounded-lg border border-stone-200 bg-white p-6">
            <h2 className="text-lg font-semibold">Récapitulatif</h2>
            <p className="mt-4 flex justify-between text-lg font-bold">
              <span>
                Total ({count} article{count > 1 ? 's' : ''})
              </span>
              <span>{formatPrice(cart.data.total)}</span>
            </p>
            <p className="mt-1 text-xs text-stone-600">Prix actuels du catalogue, confirmés au moment de la commande.</p>
            <div aria-live="polite" className="mt-4">
              {mutationError && <ErrorMessage error={mutationError} />}
            </div>
            <Link to="/commande" className="mt-4 block rounded-full bg-safran py-3.5 text-center font-bold text-stone-900 hover:bg-safran-light">
              Passer la commande
            </Link>
            <p className="mt-2.5 text-center text-[13px] text-stone-600">Vous pourrez aussi payer en espèces au livreur.</p>
          </aside>
        </div>
      )}
    </>
  )
}
