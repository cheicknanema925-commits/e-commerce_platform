import { useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { simulatePayment } from '../api/orders.ts'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { useOrder } from '../hooks/useOrders.ts'
import { formatPrice } from '../lib/format.ts'

// Développement uniquement : remplace la page hébergée par le fournisseur (Wave, Orange Money…).
// En production, le client est redirigé vers la vraie page de PayDunya et cette page n'est jamais utilisée.
export function SimulatedPaymentPage() {
  const reference = useParams().reference ?? ''
  const [searchParams] = useSearchParams()
  const orderId = Number(searchParams.get('commande'))
  const order = useOrder(orderId)
  const navigate = useNavigate()
  const [error, setError] = useState<unknown>(null)
  const [pending, setPending] = useState(false)

  async function finish(outcome: 'paye' | 'annule') {
    setPending(true)
    try {
      await simulatePayment(reference, outcome)
      // Comme un vrai fournisseur : retour vers la boutique, qui attend la confirmation du serveur
      navigate(`/commandes/${orderId}?paiement=${outcome === 'paye' ? 'retour' : 'annule'}`, { replace: true })
    } catch (e) {
      setError(e)
      setPending(false)
    }
  }

  return (
    <div className="mx-auto max-w-[420px] py-6">
      <p className="rounded-lg bg-safran-soft p-3 text-sm font-medium text-safran-ink">
        Page de paiement <strong>simulée</strong> (environnement de développement) : aucun argent réel.
      </p>
      <div className="mt-4 rounded-[18px] border border-stone-200 bg-white p-7 text-center">
        <h1 className="text-2xl font-bold">Paiement mobile</h1>
        <p className="mt-2 text-stone-600">Commande n° {orderId}</p>
        {order.data && <p className="mt-4 text-3xl font-extrabold tracking-tight">{formatPrice(order.data.totalAmount)}</p>}
        {error !== null && (
          <div className="mt-4">
            <ErrorMessage error={error} />
          </div>
        )}
        <div className="mt-6 flex flex-col gap-3">
          <button
            type="button"
            disabled={pending}
            onClick={() => finish('paye')}
            className="rounded-full bg-safran py-3.5 font-bold text-stone-900 hover:bg-safran-light disabled:opacity-60"
          >
            Valider le paiement
          </button>
          <button
            type="button"
            disabled={pending}
            onClick={() => finish('annule')}
            className="rounded-full border border-stone-200 py-3 font-semibold hover:border-bissap hover:text-bissap"
          >
            Annuler
          </button>
        </div>
      </div>
    </div>
  )
}
