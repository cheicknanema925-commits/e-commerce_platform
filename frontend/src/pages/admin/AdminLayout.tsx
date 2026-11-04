import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../../auth/useAuth.ts'
import { Icon, type IconName } from '../../components/Icon.tsx'
import { LogoMark } from '../../components/Logo.tsx'
import { useStats } from '../../hooks/useAdmin.ts'
import { initials } from '../../lib/admin.ts'
import { pendingActionCount } from '../../lib/orderViews.ts'

type NavItem = { to: string; label: string; icon: IconName; end?: boolean; badge?: 'orders' | 'cash' }

const SECTIONS: { title: string; items: NavItem[] }[] = [
  {
    title: 'Boutique',
    items: [
      { to: '/admin', label: 'Vue d’ensemble', icon: 'dashboard', end: true },
      { to: '/admin/produits', label: 'Produits', icon: 'box', end: true },
      { to: '/admin/produits/nouveau', label: 'Ajouter un article', icon: 'addCircle' },
      { to: '/admin/commandes', label: 'Commandes', icon: 'bag', badge: 'orders' },
      { to: '/admin/paiements', label: 'Paiements', icon: 'cash', badge: 'cash' },
      { to: '/admin/analyse', label: 'Analyse des ventes', icon: 'chart' },
    ],
  },
  { title: 'Gestion', items: [{ to: '/admin/clients', label: 'Clients', icon: 'users' }] },
]

// Fil d'Ariane de la barre du haut : le libellé de la rubrique la plus précise qui correspond à l'adresse
function pageTitle(pathname: string): string {
  if (pathname === '/admin/produits/nouveau') {
    return 'Ajouter un article'
  }
  if (pathname.startsWith('/admin/produits/')) {
    return 'Modifier un article'
  }
  const item = SECTIONS.flatMap((s) => s.items)
    .filter((i) => (i.end ? pathname === i.to : pathname.startsWith(i.to)))
    .sort((a, b) => b.to.length - a.to.length)[0]
  return item?.label ?? 'Administration'
}

const navClass = ({ isActive }: { isActive: boolean }) =>
  `relative flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-[15px] max-md:justify-center max-md:px-0 ${
    isActive ? 'bg-white font-semibold text-brand-700' : 'text-brand-100 hover:bg-white/10 hover:text-white'
  }`

// Espace d'administration : son propre cadre, distinct de la boutique (maquette « espace vendeur »)
export function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const stats = useStats()
  const pending = stats.data ? pendingActionCount(stats.data) : 0
  const cashOrders = stats.data?.payments.cashToCollectOrders ?? 0
  const badges = { orders: pending, cash: cashOrders }
  const name = user?.fullName ?? 'Administrateur'

  async function onLogout() {
    await logout()
    navigate('/')
  }

  return (
    <div className="min-h-screen bg-stone-100">
      <a href="#contenu-admin" className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-20 focus:z-50 focus:rounded focus:bg-white focus:px-4 focus:py-2">
        Aller au contenu
      </a>

      <aside aria-label="Navigation de l'administration" className="fixed inset-y-0 left-0 z-10 flex w-16 flex-col overflow-y-auto bg-brand-900 px-2 py-4 text-brand-200 md:w-62 md:px-3.5 md:py-5.5">
        <Link to="/admin" className="flex items-center gap-2.5 px-1.5 pb-1.5 text-[22px] font-extrabold tracking-tight text-white md:px-2.5">
          <LogoMark dark size={32} />
          <span className="max-md:sr-only">
            Shop <span className="text-safran">Africa</span>
          </span>
        </Link>
        <p className="px-2.5 pb-5.5 text-[13px] text-brand-300 max-md:sr-only">Espace vendeur</p>

        <nav aria-label="Administration" className="flex-1">
          {SECTIONS.map((section) => (
            <div key={section.title}>
              <p className="px-3 pt-3.5 pb-1.5 text-[13px] text-brand-300 max-md:sr-only">{section.title}</p>
              <ul className="space-y-0.5">
                {section.items.map((item) => {
                  const count = item.badge ? badges[item.badge] : 0
                  return (
                    <li key={item.to}>
                      <NavLink to={item.to} end={item.end} className={navClass} title={item.label}>
                        <Icon name={item.icon} className="size-5" />
                        <span className="max-md:sr-only">{item.label}</span>
                        {count > 0 && (
                          <span className="rounded-full bg-safran px-2 py-px text-xs font-bold text-stone-900 max-md:absolute max-md:top-0.5 max-md:right-0.5 max-md:px-1.5 max-md:text-[11px] md:ml-auto">
                            {count}
                            <span className="sr-only">{item.badge === 'cash' ? ' encaissements à suivre' : ' à traiter'}</span>
                          </span>
                        )}
                      </NavLink>
                    </li>
                  )
                })}
              </ul>
            </div>
          ))}
        </nav>

        <div className="mt-4 space-y-0.5 border-t border-white/10 pt-3.5">
          <Link to="/" className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-brand-100 hover:bg-white/10 hover:text-white max-md:justify-center max-md:px-0" title="Voir la boutique">
            <Icon name="store" className="size-5" />
            <span className="max-md:sr-only">Voir la boutique</span>
          </Link>
          <button
            type="button"
            onClick={onLogout}
            title="Déconnexion"
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-brand-100 hover:bg-white/10 hover:text-white max-md:justify-center max-md:px-0"
          >
            <Icon name="logout" className="size-5" />
            <span className="max-md:sr-only">Déconnexion</span>
          </button>
          <div className="flex items-center gap-2.5 px-1.5 pt-3 md:px-2.5">
            <span aria-hidden="true" className="grid size-9 shrink-0 place-items-center rounded-full bg-safran text-sm font-bold text-stone-900">
              {initials(name)}
            </span>
            <span className="min-w-0 max-md:sr-only">
              <b className="block truncate text-sm font-semibold text-white">{name}</b>
              <small className="text-[13px] text-brand-300">Administrateur</small>
            </span>
          </div>
        </div>
      </aside>

      <div className="ml-16 min-w-0 md:ml-62">
        <header className="sticky top-0 z-[8] flex h-16 items-center justify-between border-b border-stone-200 bg-white px-4 md:px-8">
          <p className="text-sm text-stone-600">
            Shop Africa / <b className="font-semibold text-stone-900">{pageTitle(location.pathname)}</b>
          </p>
          <div className="flex items-center gap-3.5 text-sm">
            <Link
              to="/admin#a-traiter"
              aria-label={pending > 0 ? `${pending} commandes à traiter` : 'Aucune commande à traiter'}
              className="relative grid size-9.5 place-items-center rounded-full bg-stone-100 hover:bg-stone-200"
            >
              <Icon name="bell" className="size-[19px]" />
              {pending > 0 && <span aria-hidden="true" className="absolute top-2 right-2.5 size-2 rounded-full border-2 border-stone-100 bg-bissap" />}
            </Link>
            <span className="max-sm:hidden">
              Bonjour, <b className="font-semibold">{name.split(' ')[0]}</b>
            </span>
          </div>
        </header>
        <main id="contenu-admin" className="mx-auto max-w-[1400px] px-4 pt-5 pb-14 md:px-8 md:pt-7">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
