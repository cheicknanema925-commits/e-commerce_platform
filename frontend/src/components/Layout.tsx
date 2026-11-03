import { Link, NavLink, Outlet } from 'react-router'

// Cadre commun à toutes les pages : en-tête, contenu de la page courante (<Outlet />), pied de page
export function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <a
        href="#contenu"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:rounded focus:bg-white focus:px-4 focus:py-2"
      >
        Aller au contenu
      </a>

      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4">
          <Link to="/" className="text-xl font-bold tracking-tight text-brand-800">
            Boutique
          </Link>
          <nav aria-label="Navigation principale">
            <NavLink
              to="/"
              end
              className={({ isActive }) =>
                `rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-brand-50 text-brand-800' : 'text-stone-700 hover:bg-stone-100'}`
              }
            >
              Catalogue
            </NavLink>
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
