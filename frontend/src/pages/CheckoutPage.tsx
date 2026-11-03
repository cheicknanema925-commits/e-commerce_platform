import { type FormEvent, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import type { PaymentMethod } from '../api/types.ts'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { FormField } from '../components/FormField.tsx'
import { useCart } from '../hooks/useCart.ts'
import { useCheckout, useStartPayment } from '../hooks/useOrders.ts'
import { formatPrice } from '../lib/format.ts'
import { fieldErrors } from '../lib/forms.ts'
import { PAYMENT_METHOD_LABELS } from '../lib/labels.ts'

type Position = { latitude: number; longitude: number }

export function CheckoutPage() {
  const cart = useCart()
  const checkout = useCheckout()
  const startPayment = useStartPayment()
  const navigate = useNavigate()
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('ONLINE')
  const [position, setPosition] = useState<Position | null>(null)
  const [locating, setLocating] = useState(false)
  const errors = fieldErrors(checkout.error)

  if (cart.isPending) {
    return <p className="text-stone-600">Chargement…</p>
  }
  if (cart.data?.items.length === 0 && !checkout.isSuccess) {
    return <Navigate to="/panier" replace />
  }

  // Le point GPS aide le livreur quand l'adresse seule ne suffit pas (facultatif, demandé au navigateur)
  function locate() {
    setLocating(true)
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setPosition({ latitude: round(coords.latitude), longitude: round(coords.longitude) })
        setLocating(false)
      },
      () => setLocating(false),
      { enableHighAccuracy: true, timeout: 10_000 },
    )
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const order = await checkout.mutateAsync({
      paymentMethod,
      delivery: {
        addressLine: String(form.get('addressLine')),
        city: String(form.get('city')),
        phone: String(form.get('phone')),
        ...position,
      },
    })
    if (paymentMethod === 'ONLINE') {
      // On quitte le site vers la page du fournisseur : aucune donnée de paiement ne passe par chez nous
      const payment = await startPayment.mutateAsync(order.id)
      window.location.assign(payment.checkoutUrl)
    } else {
      navigate(`/commandes/${order.id}?confirmation=1`)
    }
  }

  const pending = checkout.isPending || startPayment.isPending
  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight">Commande</h1>
      <form onSubmit={(event) => onSubmit(event).catch(() => undefined)} className="mt-6 grid gap-8 lg:grid-cols-3">
        <div className="flex flex-col gap-6 lg:col-span-2">
          <fieldset className="flex flex-col gap-4 rounded-lg border border-stone-200 bg-white p-6">
            <legend className="px-1 text-lg font-semibold">Adresse de livraison</legend>
            <FormField
              name="addressLine"
              label="Adresse"
              autoComplete="street-address"
              required
              maxLength={255}
              placeholder="Ex. : Rue 10, Sicap Liberté 6, près de la pharmacie"
              error={errors['delivery.addressLine']}
            />
            <FormField name="city" label="Ville" autoComplete="address-level2" required maxLength={100} defaultValue="Dakar" error={errors['delivery.city']} />
            <FormField
              name="phone"
              label="Téléphone"
              type="tel"
              autoComplete="tel"
              required
              pattern="^\+?[0-9 ]{9,20}$"
              placeholder="+221 77 123 45 67"
              error={errors['delivery.phone']}
            />
            <div>
              <button
                type="button"
                onClick={locate}
                disabled={locating}
                className="rounded-md border border-stone-300 px-3 py-2 text-sm font-medium hover:bg-stone-100"
              >
                {locating ? 'Localisation…' : 'Utiliser ma position (facultatif)'}
              </button>
              <p aria-live="polite" className="mt-1 text-xs text-stone-500">
                {position && `Position enregistrée : ${position.latitude}, ${position.longitude}`}
              </p>
            </div>
          </fieldset>

          <fieldset className="flex flex-col gap-3 rounded-lg border border-stone-200 bg-white p-6">
            <legend className="px-1 text-lg font-semibold">Paiement</legend>
            {(Object.keys(PAYMENT_METHOD_LABELS) as PaymentMethod[]).map((method) => (
              <label key={method} className="flex cursor-pointer items-center gap-3 rounded-md border border-stone-200 p-3 has-[:checked]:border-brand-700 has-[:checked]:bg-brand-50">
                <input
                  type="radio"
                  name="paymentMethod"
                  value={method}
                  checked={paymentMethod === method}
                  onChange={() => setPaymentMethod(method)}
                />
                {PAYMENT_METHOD_LABELS[method]}
              </label>
            ))}
          </fieldset>
        </div>

        <aside className="h-fit rounded-lg border border-stone-200 bg-white p-6">
          <h2 className="text-lg font-semibold">Votre commande</h2>
          <ul className="mt-4 space-y-2 text-sm">
            {cart.data?.items.map((item) => (
              <li key={item.productId} className="flex justify-between gap-2">
                <span>
                  {item.quantity} × {item.productName}
                </span>
                <span>{formatPrice(item.subtotal)}</span>
              </li>
            ))}
          </ul>
          <p className="mt-4 flex justify-between border-t border-stone-200 pt-4 text-lg">
            <span>Total</span>
            <span className="font-bold text-brand-800">{formatPrice(cart.data?.total ?? 0)}</span>
          </p>
          <div aria-live="polite" className="mt-4">
            {checkout.isError && Object.keys(errors).length === 0 && <ErrorMessage error={checkout.error} />}
            {startPayment.isError && <ErrorMessage error={startPayment.error} />}
          </div>
          <button
            type="submit"
            disabled={pending}
            className="mt-4 w-full rounded-md bg-brand-700 px-4 py-3 font-medium text-white hover:bg-brand-800 disabled:opacity-60"
          >
            {pending ? 'Validation…' : paymentMethod === 'ONLINE' ? 'Commander et payer' : 'Confirmer la commande'}
          </button>
          <Link to="/panier" className="mt-3 block text-center text-sm text-brand-700 hover:underline">
            Modifier le panier
          </Link>
        </aside>
      </form>
    </>
  )
}

function round(value: number): number {
  return Math.round(value * 1_000_000) / 1_000_000
}
