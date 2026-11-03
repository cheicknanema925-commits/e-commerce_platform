import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth.ts'
import { useCart } from '../hooks/useCart.ts'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-brand-50 text-brand-800' : 'text-stone-700 hover:bg-stone-100'}`

// Cadre commun à toutes les pages : en-tête, contenu de la page courante (<Outlet />), pied de page
export function Layout() {
  const { status, user, logout } = useAuth()
  const cart = useCart()
  const navigate = useNavigate()
  const cartCount = cart.data?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0

  async function onLogout() {
    await logout()
    navigate('/')
  }

  return (
    <div className="flex min-h-screen flex-col">
      <a
        href="#contenu"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:rounded focus:bg-white focus:px-4 focus:py-2"
      >
        Aller au contenu
      </a>

      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-4">
          <Link to="/" className="text-xl font-bold tracking-tight text-brand-800">
            Boutique
          </Link>
          <nav aria-label="Navigation principale" className="flex flex-wrap items-center gap-1">
            <NavLink to="/" end className={navLinkClass}>
              Catalogue
            </NavLink>
            {status === 'authenticated' && (
              <>
                {user?.role === 'ADMIN' && (
                  <NavLink to="/admin" className={navLinkClass}>
                    Administration
                  </NavLink>
                )}
                <NavLink to="/commandes" className={navLinkClass}>
                  Mes commandes
                </NavLink>
                <NavLink to="/panier" className={navLinkClass}>
                  Panier
                  {cartCount > 0 && (
                    <span className="ml-1 rounded-full bg-brand-700 px-2 py-0.5 text-xs text-white">
                      {cartCount}
                      <span className="sr-only"> article{cartCount > 1 ? 's' : ''}</span>
                    </span>
                  )}
                </NavLink>
                <span className="px-2 text-sm text-stone-600">{user?.fullName.split(' ')[0]}</span>
                <button type="button" onClick={onLogout} className="rounded-md px-3 py-2 text-sm font-medium text-stone-700 hover:bg-stone-100">
                  Déconnexion
                </button>
              </>
            )}
            {status === 'anonymous' && (
              <>
                <NavLink to="/connexion" className={navLinkClass}>
                  Connexion
                </NavLink>
                <NavLink to="/inscription" className="rounded-md bg-brand-700 px-3 py-2 text-sm font-medium text-white hover:bg-brand-800">
                  Créer un compte
                </NavLink>
              </>
            )}
          </nav>
        </div>
      </header>

      <main id="contenu" className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <Outlet />
      </main>

      <footer className="border-t border-stone-200 bg-white py-6 text-center text-sm text-stone-600">
        Livraison à Dakar · Paiement Wave, Orange Money ou en espèces à la livraison
      </footer>
    </div>
  )
}
