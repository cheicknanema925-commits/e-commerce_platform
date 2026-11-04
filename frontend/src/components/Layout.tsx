import { type FormEvent, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate, useSearchParams } from 'react-router'
import { useAuth } from '../auth/useAuth.ts'
import { useCategories } from '../hooks/useCatalog.ts'
import { useCart } from '../hooks/useCart.ts'
import { CartDrawer } from './CartDrawer.tsx'
import { Icon } from './Icon.tsx'
import { Logo } from './Logo.tsx'

const actionClass = 'relative flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm font-medium hover:bg-stone-100'

// Cadre commun à toutes les pages de la boutique : bandeau, en-tête collant, rayons, contenu (<Outlet />), pied de page
export function Layout() {
  const { status, user, logout } = useAuth()
  const cart = useCart()
  const categories = useCategories()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const [cartOpen, setCartOpen] = useState(false)
  const cartCount = cart.data?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0
  const onCatalog = location.pathname === '/'
  const currentCategory = onCatalog ? searchParams.get('category') : null

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const q = String(new FormData(event.currentTarget).get('q') ?? '').trim()
    navigate(q ? `/?${new URLSearchParams({ q })}` : '/')
  }

  async function onLogout() {
    await logout()
    navigate('/')
  }

  const categoryLinkClass = (active: boolean) =>
    `border-b-3 px-3.5 py-3 text-sm font-medium whitespace-nowrap ${
      active ? 'border-safran font-semibold text-brand-700' : 'border-transparent text-stone-600 hover:text-stone-900'
    }`

  return (
    <div className="flex min-h-screen flex-col">
      <a href="#contenu" className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:rounded focus:bg-white focus:px-4 focus:py-2">
        Aller au contenu
      </a>

      <div className="bg-brand-900 text-[13px] text-brand-100">
        <div className="mx-auto flex max-w-[1200px] justify-between gap-4 px-4 py-2 sm:px-6">
          <p>
            Livraison partout au Sénégal, paiement <strong className="font-semibold text-safran">Wave</strong>,{' '}
            <strong className="font-semibold text-safran">Orange Money</strong> ou{' '}
            <strong className="font-semibold text-safran">en espèces à la livraison</strong>
          </p>
          <a href="#aide" className="hidden hover:text-white md:block">
            Aide et service client
          </a>
        </div>
      </div>

      <header className="sticky top-0 z-20 border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-[1200px] flex-wrap items-center gap-3 px-4 py-4 sm:px-6 md:flex-nowrap md:gap-7">
          <Link to="/" aria-label="Shop Africa, accueil">
            <Logo />
          </Link>

          <form
            role="search"
            onSubmit={onSearch}
            className="order-3 flex w-full min-w-0 items-center rounded-full border-2 border-transparent bg-stone-100 py-1 pr-1 pl-4 transition focus-within:border-brand-700 focus-within:bg-white md:order-none md:max-w-[640px] md:flex-1"
          >
            <label htmlFor="recherche" className="sr-only">
              Rechercher un produit
            </label>
            <input
              id="recherche"
              name="q"
              type="search"
              maxLength={100}
              key={searchParams.get('q') ?? ''}
              defaultValue={onCatalog ? (searchParams.get('q') ?? '') : ''}
              placeholder="Rechercher un produit"
              autoComplete="off"
              className="w-0 min-w-0 flex-1 bg-transparent px-1.5 py-2 text-[15px] outline-none"
            />
            <button type="submit" className="rounded-full bg-brand-700 px-5 py-2 text-sm font-semibold text-white hover:bg-brand-900">
              Rechercher
            </button>
          </form>

          <nav aria-label="Mon compte" className="ml-auto flex items-center gap-1">
            {status === 'authenticated' ? (
              <>
                {user?.role === 'ADMIN' && (
                  <Link to="/admin" className={actionClass}>
                    <Icon name="dashboard" className="size-[22px]" />
                    <span className="hidden lg:inline">Administration</span>
                    <span className="sr-only lg:hidden">Administration</span>
                  </Link>
                )}
                <NavLink to="/commandes" className={actionClass}>
                  <Icon name="bag" className="size-[22px]" />
                  <span className="hidden lg:inline">Mes commandes</span>
                  <span className="sr-only lg:hidden">Mes commandes</span>
                </NavLink>
                <span className="hidden px-2 text-sm text-stone-600 xl:inline">
                  Bonjour, <b className="font-semibold text-stone-900">{user?.fullName.split(' ')[0]}</b>
                </span>
                <button type="button" onClick={onLogout} className={actionClass}>
                  <Icon name="logout" className="size-[22px]" />
                  <span className="hidden lg:inline">Déconnexion</span>
                  <span className="sr-only lg:hidden">Déconnexion</span>
                </button>
                <button type="button" onClick={() => setCartOpen(true)} className={actionClass}>
                  <Icon name="cart" className="size-[22px]" />
                  <span className="hidden sm:inline">Panier</span>
                  <span className="sr-only sm:hidden">Panier</span>
                  {cartCount > 0 && (
                    <span className="absolute top-0.5 left-6 grid h-[18px] min-w-[18px] place-items-center rounded-full bg-safran px-1.5 text-[11px] font-bold text-stone-900">
                      {cartCount}
                      <span className="sr-only"> article{cartCount > 1 ? 's' : ''}</span>
                    </span>
                  )}
                </button>
              </>
            ) : (
              status === 'anonymous' && (
                <>
                  <Link to="/connexion" className={actionClass}>
                    <Icon name="user" className="size-[22px]" />
                    <span>Compte</span>
                  </Link>
                  <Link to="/connexion?retour=%2Fpanier" className={actionClass}>
                    <Icon name="cart" className="size-[22px]" />
                    <span className="hidden sm:inline">Panier</span>
                    <span className="sr-only sm:hidden">Panier</span>
                  </Link>
                </>
              )
            )}
          </nav>
        </div>

        <nav aria-label="Rayons" className="border-t border-stone-200">
          <div className="mx-auto flex max-w-[1200px] gap-1 overflow-x-auto px-4 [scrollbar-width:none] sm:px-6">
            <Link to="/" aria-current={onCatalog && !currentCategory ? 'page' : undefined} className={categoryLinkClass(onCatalog && !currentCategory)}>
              Tous les produits
            </Link>
            {categories.data?.map((category) => (
              <Link
                key={category.id}
                to={`/?category=${category.slug}`}
                aria-current={currentCategory === category.slug ? 'page' : undefined}
                className={categoryLinkClass(currentCategory === category.slug)}
              >
                {category.name}
              </Link>
            ))}
          </div>
        </nav>
      </header>

      <main id="contenu" className="mx-auto w-full max-w-[1200px] flex-1 px-4 py-7 sm:px-6">
        <Outlet />
      </main>

      <footer id="aide" className="mt-16 bg-brand-900 text-sm text-brand-200">
        <div className="mx-auto grid max-w-[1200px] gap-7 px-4 pt-11 pb-7 sm:grid-cols-2 sm:px-6 lg:grid-cols-[1.4fr_1fr_1fr_1fr]">
          <div>
            <Logo dark />
            <p className="mt-3">Le marché du quotidien, en ligne. Des produits utiles, des prix clairs, une livraison suivie.</p>
          </div>
          <div>
            <h3 className="mb-2.5 text-[15px] font-semibold text-white">Shop Africa</h3>
            <ul className="space-y-1.5">
              <li>
                <Link to="/" className="hover:text-white hover:underline">
                  Tous les produits
                </Link>
              </li>
              <li>
                <Link to="/inscription" className="hover:text-white hover:underline">
                  Créer un compte
                </Link>
              </li>
            </ul>
          </div>
          <div>
            <h3 className="mb-2.5 text-[15px] font-semibold text-white">Aide</h3>
            <ul className="space-y-1.5">
              <li>
                <Link to="/commandes" className="hover:text-white hover:underline">
                  Suivre ma commande
                </Link>
              </li>
              <li>Livraison à domicile partout au Sénégal</li>
            </ul>
          </div>
          <div>
            <h3 className="mb-2.5 text-[15px] font-semibold text-white">Paiement</h3>
            <ul className="space-y-1.5">
              <li>Wave</li>
              <li>Orange Money</li>
              <li>Carte bancaire</li>
              <li>Espèces à la livraison</li>
            </ul>
          </div>
        </div>
        <div className="mx-auto max-w-[1200px] border-t border-white/10 px-4 pt-4 pb-5 text-[13px] sm:px-6">© 2026 Shop Africa</div>
      </footer>

      {status === 'authenticated' && <CartDrawer open={cartOpen} onClose={() => setCartOpen(false)} />}
    </div>
  )
}
