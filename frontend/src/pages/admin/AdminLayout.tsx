import { Link, NavLink, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../../auth/useAuth.ts'
import { AdminIcon, type AdminIconName } from '../../components/admin/AdminIcon.tsx'
import { useStats } from '../../hooks/useAdmin.ts'
import { pendingActionCount } from '../../lib/orderViews.ts'

const NAV: { to: string; label: string; icon: AdminIconName; end?: boolean }[] = [
  { to: '/admin', label: 'Tableau de bord', icon: 'dashboard', end: true },
  { to: '/admin/commandes', label: 'Commandes', icon: 'orders' },
  { to: '/admin/produits', label: 'Produits', icon: 'products' },
  { to: '/admin/clients', label: 'Clients', icon: 'customers' },
]

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `flex shrink-0 items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition ${
    isActive ? 'bg-stone-800 text-white' : 'text-stone-400 hover:bg-stone-800/60 hover:text-white'
  }`

// Espace d'administration : son propre cadre (menu latéral sombre), distinct de celui de la boutique
export function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const stats = useStats()
  const pending = stats.data ? pendingActionCount(stats.data) : 0

  async function onLogout() {
    await logout()
    navigate('/')
  }

  return (
    <div className="min-h-screen bg-stone-100 lg:flex">
      <a
        href="#contenu-admin"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded focus:bg-white focus:px-4 focus:py-2"
      >
        Aller au contenu
      </a>

      <aside className="flex flex-col bg-stone-900 lg:fixed lg:inset-y-0 lg:w-64">
        <div className="flex items-center justify-between gap-4 px-5 py-4 lg:py-6">
          <Link to="/admin" className="leading-tight">
            <span className="block text-lg font-bold tracking-tight text-white">Boutique</span>
            <span className="block text-xs font-medium uppercase tracking-widest text-brand-400">Back-office</span>
          </Link>
        </div>

        <nav aria-label="Administration" className="flex gap-1 overflow-x-auto px-3 pb-3 lg:flex-1 lg:flex-col lg:overflow-visible">
          {NAV.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={navLinkClass}>
              <AdminIcon name={item.icon} />
              {item.label}
              {item.to === '/admin/commandes' && pending > 0 && (
                <span className="ml-auto rounded-full bg-amber-400 px-2 py-0.5 text-xs font-semibold text-stone-900">
                  {pending}
                  <span className="sr-only"> à traiter</span>
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="hidden border-t border-stone-800 p-3 lg:block">
          <Link to="/" className="flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-stone-400 hover:bg-stone-800/60 hover:text-white">
            <AdminIcon name="store" />
            Voir la boutique
          </Link>
          <p className="mt-3 truncate px-3 text-sm text-stone-300">{user?.fullName}</p>
          <p className="truncate px-3 text-xs text-stone-500">{user?.email}</p>
          <button
            type="button"
            onClick={onLogout}
            className="mt-2 flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-stone-400 hover:bg-stone-800/60 hover:text-white"
          >
            <AdminIcon name="logout" />
            Déconnexion
          </button>
        </div>
      </aside>

      <div className="flex-1 lg:pl-64">
        {/* Sur mobile, le bas du menu latéral devient une barre sous le menu */}
        <div className="flex items-center justify-between gap-4 border-b border-stone-200 bg-white px-4 py-2 text-sm lg:hidden">
          <Link to="/" className="font-medium text-brand-700">
            Voir la boutique
          </Link>
          <button type="button" onClick={onLogout} className="font-medium text-stone-700">
            Déconnexion
          </button>
        </div>
        <main id="contenu-admin" className="mx-auto w-full max-w-7xl px-4 py-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
