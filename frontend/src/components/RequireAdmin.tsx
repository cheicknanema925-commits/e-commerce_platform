import { Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth.ts'

// Comme hasRole('ADMIN') : réservé aux administrateurs. Simple confort d'affichage :
// même en contournant cette page, l'API refuserait chaque appel /api/admin/** (403).
export function RequireAdmin() {
  const { user } = useAuth()
  if (user?.role !== 'ADMIN') {
    return (
      <p role="alert" className="rounded-md border border-stone-200 bg-white p-6 text-stone-700">
        Cette partie du site est réservée aux administrateurs.
      </p>
    )
  }
  return <Outlet />
}
