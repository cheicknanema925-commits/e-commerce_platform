import { NavLink, Outlet } from 'react-router'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium ${isActive ? 'bg-stone-900 text-white' : 'text-stone-700 hover:bg-stone-200'}`

export function AdminLayout() {
  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-stone-200 pb-4">
        <h1 className="text-2xl font-bold tracking-tight">Administration</h1>
        <nav aria-label="Administration" className="flex gap-1">
          <NavLink to="/admin" end className={linkClass}>
            Tableau de bord
          </NavLink>
          <NavLink to="/admin/produits" className={linkClass}>
            Produits
          </NavLink>
          <NavLink to="/admin/commandes" className={linkClass}>
            Commandes
          </NavLink>
        </nav>
      </div>
      <div className="mt-6">
        <Outlet />
      </div>
    </>
  )
}
