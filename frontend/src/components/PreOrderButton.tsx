import { Link, useLocation } from 'react-router'
import type { Product } from '../api/types.ts'
import { useAuth } from '../auth/useAuth.ts'
import { useMyPreOrders, usePreOrder } from '../hooks/useSupport.ts'
import { useToast } from '../lib/toast.ts'
import { ErrorMessage } from './ErrorMessage.tsx'

// Produit en rupture : « Prévenez-moi à son retour » (pré-commande : rien n'est payé ni réservé)
export function PreOrderButton({ product }: { product: Product }) {
  const { status } = useAuth()
  const location = useLocation()
  const preOrders = useMyPreOrders()
  const preOrder = usePreOrder()
  const toast = useToast()

  if (product.stock > 0) {
    return null
  }
  if (status !== 'authenticated') {
    return (
      <Link
        to={`/connexion?retour=${encodeURIComponent(location.pathname)}`}
        className="mt-6 inline-block rounded-full border-2 border-brand-700 px-6 py-3 font-semibold text-brand-700 hover:bg-brand-50"
      >
        Se connecter pour être prévenu du retour
      </Link>
    )
  }
  const waiting = preOrders.data?.some((p) => p.productId === product.id && p.status === 'WAITING')
  return (
    <div className="mt-6" aria-live="polite">
      {waiting ? (
        <p className="rounded-lg bg-kola-soft p-3 text-sm font-medium text-kola">
          Vous serez prévenu dès son retour en stock. <Link to="/demandes" className="underline underline-offset-2">Voir mes demandes</Link>
        </p>
      ) : (
        <button
          type="button"
          disabled={preOrder.isPending}
          onClick={() => preOrder.mutate({ productId: product.id, quantity: 1 }, { onSuccess: () => toast(`Vous serez prévenu du retour de ${product.name}`) })}
          className="rounded-full border-2 border-brand-700 px-6 py-3 font-semibold text-brand-700 hover:bg-brand-50 disabled:opacity-60"
        >
          {preOrder.isPending ? 'Enregistrement…' : 'Me prévenir de son retour'}
        </button>
      )}
      {preOrder.isError && (
        <div className="mt-3">
          <ErrorMessage error={preOrder.error} />
        </div>
      )}
    </div>
  )
}
