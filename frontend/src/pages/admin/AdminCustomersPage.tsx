import { type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { AmountKpi, filterInput, Kpi, KpiStrip, PageHeader, TableCard, td, th } from '../../components/admin/ui.tsx'
import { useCustomers, useStats } from '../../hooks/useAdmin.ts'
import { initials } from '../../lib/admin.ts'
import { formatDate, formatNumber, formatPrice } from '../../lib/format.ts'

export function AdminCustomersPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? undefined
  const page = Number(searchParams.get('page') ?? 0)
  const customers = useCustomers(q, page)
  const stats = useStats()

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const value = String(new FormData(event.currentTarget).get('q') ?? '').trim()
    setSearchParams(value ? { q: value } : {})
  }

  return (
    <>
      <PageHeader title="Gestion des clients" description="Aperçu des clients inscrits sur la boutique." />

      {stats.data && (
        <KpiStrip>
          <Kpi label="Clients inscrits" value={formatNumber(stats.data.customerCount)} />
          <Kpi label="Nouveaux ce mois" value={formatNumber(stats.data.customers.newThisMonth)} />
          <Kpi label="Clients fidèles" value={formatNumber(stats.data.customers.repeat)} detail="Au moins 2 commandes" />
          <AmountKpi label="Panier moyen" amount={stats.data.averageOrderValue} />
        </KpiStrip>
      )}

      <form role="search" onSubmit={onSearch}>
        <label htmlFor="recherche-client" className="sr-only">
          Rechercher un client
        </label>
        <input id="recherche-client" name="q" type="search" key={q} defaultValue={q} maxLength={100} placeholder="Rechercher par nom ou e-mail" className={`${filterInput} w-full sm:w-80`} />
      </form>

      {customers.isError && <ErrorMessage error={customers.error} />}
      <TableCard title="Clients" count={customers.data?.totalElements}>
        <table className={`w-full text-sm ${customers.isPlaceholderData ? 'opacity-60' : ''}`}>
          <thead>
            <tr>
              <th scope="col" className={th}>Client</th>
              <th scope="col" className={th}>E-mail</th>
              <th scope="col" className={th}>Inscrit le</th>
              <th scope="col" className={`${th} text-right`}>Commandes</th>
              <th scope="col" className={`${th} text-right`}>Total commandé</th>
              <th scope="col" className={th}>Dernière commande</th>
            </tr>
          </thead>
          <tbody>
            {customers.data?.content.map((customer) => (
              <tr key={customer.id} className="hover:bg-stone-50">
                <td className={td}>
                  <div className="flex items-center gap-2.5">
                    <span aria-hidden="true" className="grid size-8 shrink-0 place-items-center rounded-full bg-brand-100 text-xs font-bold text-brand-700">
                      {initials(customer.fullName)}
                    </span>
                    <b className="font-semibold">{customer.fullName}</b>
                  </div>
                </td>
                <td className={td}>{customer.email}</td>
                <td className={td}>{formatDate(customer.createdAt)}</td>
                <td className={`${td} text-right tabular-nums`}>
                  {customer.orderCount > 0 ? (
                    <Link to={`/admin/commandes?q=${encodeURIComponent(customer.email)}`} className="font-semibold text-brand-700 hover:underline">
                      {customer.orderCount}
                      <span className="sr-only"> commandes de {customer.fullName}</span>
                    </Link>
                  ) : (
                    0
                  )}
                </td>
                <td className={`${td} text-right font-semibold tabular-nums`}>{formatPrice(customer.totalSpent)}</td>
                <td className={td}>{customer.lastOrderAt ? formatDate(customer.lastOrderAt) : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {customers.data?.content.length === 0 && <p className="p-9 text-center text-stone-600">Aucun client ne correspond.</p>}
      </TableCard>
      <p className="mt-2 text-xs text-stone-600">Commandes et montants : commandes annulées exclues.</p>
      {customers.data && (
        <Pagination
          page={customers.data.page}
          totalPages={customers.data.totalPages}
          onChange={(next) => setSearchParams({ ...(q ? { q } : {}), page: String(next) })}
        />
      )}
    </>
  )
}
