import { Link } from 'react-router'
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

  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight">Panier</h1>
      {cart.data.items.length === 0 ? (
        <div className="mt-6 rounded-lg border border-dashed border-stone-300 p-8 text-center">
          <p className="text-stone-600">Votre panier est vide.</p>
          <Link to="/" className="mt-4 inline-block font-medium text-brand-700 hover:underline">
            Découvrir le catalogue
          </Link>
        </div>
      ) : (
        <div className="mt-6 grid gap-8 lg:grid-cols-3">
          <ul className="divide-y divide-stone-200 rounded-lg border border-stone-200 bg-white lg:col-span-2">
            {cart.data.items.map((item) => (
              <li key={item.productId} className="flex flex-wrap items-center gap-4 p-4">
                <div className="min-w-48 flex-1">
                  <Link to={`/produits/${item.productId}`} className="font-medium hover:underline">
                    {item.productName}
                  </Link>
                  <p className="text-sm text-stone-600">{formatPrice(item.unitPrice)} l'unité</p>
                </div>
                <label className="sr-only" htmlFor={`quantite-${item.productId}`}>
                  Quantité de {item.productName}
                </label>
                <input
                  id={`quantite-${item.productId}`}
                  type="number"
                  min={1}
                  max={99}
                  defaultValue={item.quantity}
                  key={item.quantity}
                  onBlur={(event) => {
                    const quantity = Number(event.target.value)
                    if (quantity !== item.quantity && quantity >= 1 && quantity <= 99) {
                      changeQuantity.mutate({ productId: item.productId, quantity })
                    }
                  }}
                  className="w-20 rounded-md border border-stone-300 px-2 py-1"
                />
                <p className="w-28 text-right font-semibold">{formatPrice(item.subtotal)}</p>
                <button
                  type="button"
                  onClick={() => removeFromCart.mutate(item.productId)}
                  className="text-sm font-medium text-red-700 hover:underline"
                >
                  Retirer<span className="sr-only"> {item.productName}</span>
                </button>
              </li>
            ))}
          </ul>

          <aside className="h-fit rounded-lg border border-stone-200 bg-white p-6">
            <h2 className="text-lg font-semibold">Récapitulatif</h2>
            <p className="mt-4 flex justify-between text-lg">
              <span>Total</span>
              <span className="font-bold text-brand-800">{formatPrice(cart.data.total)}</span>
            </p>
            <p className="mt-1 text-xs text-stone-500">Prix actuels du catalogue, confirmés au moment de la commande.</p>
            <div aria-live="polite" className="mt-4">
              {mutationError && <ErrorMessage error={mutationError} />}
            </div>
            <Link
              to="/commande"
              className="mt-4 block rounded-md bg-brand-700 px-4 py-3 text-center font-medium text-white hover:bg-brand-800"
            >
              Passer la commande
            </Link>
          </aside>
        </div>
      )}
    </>
  )
}
