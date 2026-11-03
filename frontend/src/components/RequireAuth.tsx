import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth/useAuth.ts'

// Comme .authenticated() dans SecurityConfig : les pages enfants exigent une connexion.
// C'est un confort d'affichage : la vraie protection reste l'API, qui revérifie le jeton à chaque requête.
export function RequireAuth() {
  const { status } = useAuth()
  const location = useLocation()
  if (status === 'loading') {
    return <p className="text-stone-600">Chargement…</p>
  }
  if (status === 'anonymous') {
    const returnTo = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/connexion?retour=${returnTo}`} replace />
  }
  return <Outlet />
}
