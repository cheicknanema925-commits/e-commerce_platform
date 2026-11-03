import { ApiError } from '../api/client.ts'

// role="alert" : le message est annoncé aussitôt par les lecteurs d'écran
export function ErrorMessage({ error }: { error: unknown }) {
  const message =
    error instanceof ApiError ? error.message : 'Impossible de joindre le serveur. Vérifiez votre connexion et réessayez.'
  return (
    <p role="alert" className="rounded-md border border-red-200 bg-red-50 p-4 text-red-800">
      {message}
    </p>
  )
}
