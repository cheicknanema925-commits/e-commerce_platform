import { Link, useSearchParams } from 'react-router'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { useUnsubscribe } from '../hooks/useShop.ts'

// Lien reçu par email (?jeton=…). Un bouton confirme : les logiciels qui ouvrent les liens des emails
// (antivirus, aperçus) ne désinscrivent ainsi personne par accident
export function UnsubscribePage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('jeton') ?? ''
  const unsubscribe = useUnsubscribe()

  return (
    <div className="mx-auto max-w-[460px] py-6 text-center">
      <h1 className="text-[28px] font-bold tracking-tight">Infolettre</h1>
      <div className="mt-5 rounded-[18px] border border-stone-200 bg-white p-7">
        {unsubscribe.isSuccess ? (
          <p role="status" className="font-medium text-kola">
            C'est fait : vous ne recevrez plus nos promotions.
          </p>
        ) : (
          <>
            <p className="text-stone-700">Vous ne souhaitez plus recevoir les promotions de la semaine ?</p>
            <button
              type="button"
              disabled={!token || unsubscribe.isPending}
              onClick={() => unsubscribe.mutate(token)}
              className="mt-5 w-full rounded-full bg-safran py-3.5 font-bold text-stone-900 hover:bg-safran-light disabled:opacity-60"
            >
              {unsubscribe.isPending ? 'Désinscription…' : 'Me désinscrire'}
            </button>
            {!token && <p className="mt-3 text-sm text-bissap">Lien incomplet : utilisez le lien reçu par email.</p>}
            {unsubscribe.isError && (
              <div className="mt-3">
                <ErrorMessage error={unsubscribe.error} />
              </div>
            )}
          </>
        )}
      </div>
      <Link to="/" className="mt-4 inline-block text-sm font-semibold text-brand-700 underline">
        Retour à la boutique
      </Link>
    </div>
  )
}
