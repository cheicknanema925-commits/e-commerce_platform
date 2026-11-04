import { ApiError } from '../api/client.ts'

// role="alert" : le message est annoncé aussitôt par les lecteurs d'écran
export function ErrorMessage({ error }: { error: unknown }) {
  const message =
    error instanceof ApiError ? error.message : 'Impossible de joindre le serveur. Vérifiez votre connexion et réessayez.'
  return (
    <p role="alert" className="rounded-lg border border-bissap/30 bg-bissap-soft p-4 text-sm font-medium text-bissap">
      {message}
    </p>
  )
}
