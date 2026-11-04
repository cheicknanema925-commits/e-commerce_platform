import { type FormEvent, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import type { PaymentMethod } from '../api/types.ts'
import { CartItemThumbnail } from '../components/CartLine.tsx'
import { DeliveryLocation } from '../components/DeliveryLocation.tsx'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { FormField } from '../components/FormField.tsx'
import { Icon, type IconName } from '../components/Icon.tsx'
import { useCart } from '../hooks/useCart.ts'
import { useCheckout, useStartPayment } from '../hooks/useOrders.ts'
import { useStoreSettings } from '../hooks/useShop.ts'
import { paymentAvailability, serviceFee } from '../lib/checkout.ts'
import { formatPrice } from '../lib/format.ts'
import { fieldErrors } from '../lib/forms.ts'
import type { Position } from '../lib/location.ts'

const PAYMENT_OPTIONS: { method: PaymentMethod; icon: IconName; title: string; text: string }[] = [
  {
    method: 'ONLINE',
    icon: 'phone',
    title: 'Paiement en ligne',
    text: 'Wave, Orange Money ou carte bancaire, sur la page sécurisée de notre partenaire de paiement.',
  },
  {
    method: 'CASH_ON_DELIVERY',
    icon: 'cash',
    title: 'Espèces à la livraison',
    text: 'Vous payez le livreur quand il vous remet le colis.',
  },
]

export function CheckoutPage() {
  const cart = useCart()
  const checkout = useCheckout()
  const startPayment = useStartPayment()
  const navigate = useNavigate()
  const [chosenMethod, setPaymentMethod] = useState<PaymentMethod>('ONLINE')
  const settings = useStoreSettings()
  const [position, setPosition] = useState<Position | null>(null)
  const errors = fieldErrors(checkout.error)

  if (cart.isPending) {
    return <p className="text-stone-600">Chargement…</p>
  }
  if (cart.data?.items.length === 0 && !checkout.isSuccess) {
    return <Navigate to="/panier" replace />
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

  const cartTotal = cart.data?.total ?? 0
  const availability = paymentAvailability(settings.data, cartTotal)
  // Mode choisi indisponible (paramètres, plafond) : on bascule sur l'autre s'il est disponible
  const paymentMethod: PaymentMethod = availability[chosenMethod].available
    ? chosenMethod
    : chosenMethod === 'ONLINE' && availability.CASH_ON_DELIVERY.available
      ? 'CASH_ON_DELIVERY'
      : chosenMethod === 'CASH_ON_DELIVERY' && availability.ONLINE.available
        ? 'ONLINE'
        : chosenMethod
  const fee = serviceFee(settings.data, paymentMethod)
  const noPayment = !availability.ONLINE.available && !availability.CASH_ON_DELIVERY.available
  const pending = checkout.isPending || startPayment.isPending
  const count = cart.data?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0
  return (
    <>
      <h1 className="text-[clamp(28px,3.4vw,40px)] font-bold tracking-tight">Finaliser ma commande</h1>
      <form onSubmit={(event) => onSubmit(event).catch(() => undefined)} className="mt-6 grid gap-6 lg:grid-cols-3">
        <div className="flex flex-col gap-6 lg:col-span-2">
          <div className="rounded-lg border border-stone-200 bg-white p-6">
          <fieldset className="flex min-w-0 flex-col gap-4">
            <legend className="mb-1 flex items-center gap-2.5 text-lg font-semibold">
              <StepNumber value={1} />
              Adresse de livraison
            </legend>
            <FormField
              name="addressLine"
              label="Adresse"
              autoComplete="street-address"
              required
              maxLength={255}
              placeholder="Ex. : Rue 10, Sicap Liberté 6, près de la pharmacie"
              hint="Ajoutez un repère connu (pharmacie, mosquée, école…) pour aider le livreur."
              error={errors['delivery.addressLine']}
            />
            <div className="grid gap-4 sm:grid-cols-2">
              <FormField name="city" label="Ville" autoComplete="address-level2" required maxLength={100} defaultValue="Dakar" error={errors['delivery.city']} />
              <FormField
                name="phone"
                label="Téléphone"
                type="tel"
                autoComplete="tel"
                required
                pattern="^\+?[0-9 ]{9,20}$"
                placeholder="+221 77 123 45 67"
                hint="Le livreur vous appelle à l'arrivée."
                error={errors['delivery.phone']}
              />
            </div>
            <DeliveryLocation onChange={setPosition} />
          </fieldset>
          </div>

          <div className="rounded-lg border border-stone-200 bg-white p-6">
          <fieldset className="flex min-w-0 flex-col gap-3">
            <legend className="mb-1 flex items-center gap-2.5 text-lg font-semibold">
              <StepNumber value={2} />
              Paiement
            </legend>
            {PAYMENT_OPTIONS.map((option) => (
              <label
                key={option.method}
                className="flex cursor-pointer items-start gap-3.5 rounded-lg border-2 border-stone-200 p-4 has-[:checked]:border-brand-700 has-[:checked]:bg-brand-50 has-[:disabled]:cursor-not-allowed has-[:disabled]:opacity-60 has-[:focus-visible]:outline-3 has-[:focus-visible]:outline-safran"
              >
                <input
                  type="radio"
                  name="paymentMethod"
                  value={option.method}
                  checked={paymentMethod === option.method}
                  disabled={!availability[option.method].available}
                  onChange={() => setPaymentMethod(option.method)}
                  className="mt-1 size-[18px] accent-brand-700"
                />
                <Icon name={option.icon} className="mt-0.5 size-6 text-brand-700" />
                <span>
                  <b className="block font-semibold">{option.title}</b>
                  <span className="text-sm text-stone-600">{option.text}</span>
                  {option.method === 'CASH_ON_DELIVERY' && availability.CASH_ON_DELIVERY.available && settings.data && (
                    <span className="mt-1 block text-[13px] text-stone-600">
                      {settings.data.codFee > 0 && `Frais de service : ${formatPrice(settings.data.codFee)}. `}
                      {settings.data.codZones.length > 0 && `Villes desservies : ${settings.data.codZones.join(', ')}.`}
                    </span>
                  )}
                  {!availability[option.method].available && (
                    <span className="mt-1 block text-[13px] font-medium text-bissap">{availability[option.method].reason}</span>
                  )}
                </span>
              </label>
            ))}
          </fieldset>
          </div>
        </div>

        <aside className="h-fit rounded-lg border border-stone-200 bg-white p-6 lg:sticky lg:top-40">
          <h2 className="text-lg font-semibold">Votre commande</h2>
          <ul className="mt-4 space-y-3 text-sm">
            {cart.data?.items.map((item) => (
              <li key={item.productId} className="flex items-center gap-3">
                <CartItemThumbnail item={item} className="size-12" />
                <span className="flex-1">
                  {item.productName}
                  <span className="block text-stone-600">Quantité : {item.quantity}</span>
                </span>
                <span className="font-semibold">{formatPrice(item.subtotal)}</span>
              </li>
            ))}
          </ul>
          {fee > 0 && (
            <p className="mt-4 flex justify-between border-t border-stone-200 pt-3 text-sm">
              <span>Frais de service (espèces)</span>
              <span>{formatPrice(fee)}</span>
            </p>
          )}
          <p className={`flex justify-between text-lg font-bold ${fee > 0 ? 'mt-2' : 'mt-4 border-t border-stone-200 pt-4'}`}>
            <span>
              Total ({count} article{count > 1 ? 's' : ''})
            </span>
            <span>{formatPrice(cartTotal + fee)}</span>
          </p>
          {noPayment && <p className="mt-3 text-sm font-medium text-bissap">Aucun moyen de paiement n'est disponible pour le moment.</p>}
          <div aria-live="polite" className="mt-4">
            {checkout.isError && Object.keys(errors).length === 0 && <ErrorMessage error={checkout.error} />}
            {startPayment.isError && <ErrorMessage error={startPayment.error} />}
          </div>
          <button
            type="submit"
            disabled={pending || noPayment}
            className="mt-4 w-full rounded-full bg-safran py-3.5 font-bold text-stone-900 hover:bg-safran-light disabled:opacity-60"
          >
            {pending ? 'Validation…' : paymentMethod === 'ONLINE' ? 'Commander et payer' : 'Confirmer la commande'}
          </button>
          <Link to="/panier" className="mt-3 block text-center text-sm font-medium text-brand-700 underline underline-offset-3">
            Modifier le panier
          </Link>
        </aside>
      </form>
    </>
  )
}

function StepNumber({ value }: { value: number }) {
  return (
    <span aria-hidden="true" className="grid size-7 place-items-center rounded-full bg-brand-700 text-sm font-bold text-white">
      {value}
    </span>
  )
}
