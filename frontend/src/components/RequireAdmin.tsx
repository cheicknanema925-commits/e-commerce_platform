import { Link, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth.ts'

// Comme hasRole('ADMIN') : réservé aux administrateurs. Simple confort d'affichage :
// même en contournant cette page, l'API refuserait chaque appel /api/admin/** (403).
export function RequireAdmin() {
  const { user } = useAuth()
  if (user?.role !== 'ADMIN') {
    return (
      <div className="flex min-h-screen items-center justify-center bg-stone-100 p-4">
        <div role="alert" className="max-w-md rounded-lg border border-stone-200 bg-white p-8 text-center">
          <h1 className="text-xl font-bold">Accès réservé</h1>
          <p className="mt-2 text-stone-600">Cette partie du site est réservée aux administrateurs.</p>
          <Link to="/" className="mt-6 inline-block rounded-md bg-brand-700 px-4 py-2 font-medium text-white hover:bg-brand-800">
            Retour à la boutique
          </Link>
        </div>
      </div>
    )
  }
  return <Outlet />
}
