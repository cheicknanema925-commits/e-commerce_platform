import { useState } from 'react'
import { geolocationErrorMessage, osmEmbedUrl, osmLink, type Position, roundCoordinate } from '../lib/location.ts'
import { Icon } from './Icon.tsx'

type Located = Position & { accuracy: number }

// Option de localisation : le point GPS aide le livreur quand l'adresse seule ne suffit pas (quartier sans nom
// de rue…). Facultatif ; le navigateur demande l'accord du client, et la position ne part qu'avec la commande.
export function DeliveryLocation({ onChange }: { onChange: (position: Position | null) => void }) {
  const [located, setLocated] = useState<Located | null>(null)
  const [locating, setLocating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // La géolocalisation n'existe que sur une page sécurisée (https, ou localhost en développement)
  const supported = typeof navigator !== 'undefined' && 'geolocation' in navigator && window.isSecureContext

  function locate() {
    setLocating(true)
    setError(null)
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        const position = { latitude: roundCoordinate(coords.latitude), longitude: roundCoordinate(coords.longitude) }
        setLocated({ ...position, accuracy: Math.round(coords.accuracy) })
        onChange(position)
        setLocating(false)
      },
      (failure) => {
        setError(geolocationErrorMessage(failure.code))
        setLocating(false)
      },
      { enableHighAccuracy: true, timeout: 15_000, maximumAge: 60_000 },
    )
  }

  function clear() {
    setLocated(null)
    onChange(null)
  }

  return (
    <div className="rounded-lg border border-stone-200 bg-stone-50 p-4">
      <div className="flex items-start gap-3">
        <Icon name="pin" className="mt-0.5 size-6 text-brand-700" />
        <div className="flex-1">
          <p className="font-semibold">Position GPS pour le livreur (facultatif)</p>
          <p className="text-sm text-stone-600">Utile si votre adresse est difficile à trouver. Elle sert uniquement à la livraison.</p>
        </div>
      </div>

      {!supported && <p className="mt-3 text-sm text-stone-600">Votre navigateur ne permet pas la localisation ici : l'adresse suffit.</p>}

      {supported && !located && (
        <button
          type="button"
          onClick={locate}
          disabled={locating}
          className="mt-3 inline-flex items-center gap-2 rounded-full border-2 border-brand-700 px-4 py-2 text-sm font-semibold text-brand-700 hover:bg-brand-700 hover:text-white disabled:opacity-60"
        >
          <Icon name="pin" className="size-4" />
          {locating ? 'Localisation en cours…' : 'Utiliser ma position actuelle'}
        </button>
      )}

      <div aria-live="polite">
        {error && (
          <p role="alert" className="mt-3 rounded-lg bg-safran-soft p-3 text-sm text-safran-ink">
            {error}
          </p>
        )}
        {located && (
          <div className="mt-3">
            <p className="flex items-center gap-2 text-sm font-medium text-kola">
              <Icon name="check" className="size-4" />
              Position enregistrée (précision ± {located.accuracy} m)
            </p>
            <iframe
              title="Carte de votre position de livraison"
              src={osmEmbedUrl(located)}
              loading="lazy"
              referrerPolicy="no-referrer"
              className="mt-2 h-56 w-full rounded-lg border border-stone-200"
            />
            <p className="mt-1 text-xs text-stone-600">
              Carte fournie par OpenStreetMap (votre position lui est transmise pour l'afficher).{' '}
              <a href={osmLink(located)} target="_blank" rel="noopener noreferrer" className="font-medium text-brand-700 underline">
                Ouvrir la carte<span className="sr-only"> (nouvel onglet)</span>
              </a>
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              <button type="button" onClick={locate} disabled={locating} className="rounded-lg border border-stone-200 bg-white px-3 py-1.5 text-sm font-medium hover:border-brand-700">
                {locating ? 'Localisation…' : 'Reprendre ma position'}
              </button>
              <button type="button" onClick={clear} className="rounded-lg border border-stone-200 bg-white px-3 py-1.5 text-sm font-medium hover:border-bissap hover:text-bissap">
                Retirer la position
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
