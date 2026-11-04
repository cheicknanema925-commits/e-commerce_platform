import { type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { PageHeader } from '../../components/admin/PageHeader.tsx'
import { useCustomers } from '../../hooks/useAdmin.ts'
import { formatDate, formatPrice } from '../../lib/format.ts'

export function AdminCustomersPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? undefined
  const page = Number(searchParams.get('page') ?? 0)
  const customers = useCustomers(q, page)

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const value = String(new FormData(event.currentTarget).get('q') ?? '').trim()
    setSearchParams(value ? { q: value } : {})
  }

  return (
    <>
      <PageHeader
        title="Clients"
        description={customers.data ? `${customers.data.totalElements} client${customers.data.totalElements > 1 ? 's' : ''} inscrit${customers.data.totalElements > 1 ? 's' : ''}` : undefined}
      />

      <form role="search" onSubmit={onSearch} className="mb-4 flex max-w-xl gap-2">
        <label htmlFor="recherche-client" className="sr-only">
          Rechercher un client
        </label>
        <input
          id="recherche-client"
          name="q"
          type="search"
          key={q}
          defaultValue={q}
          maxLength={100}
          placeholder="Nom ou email"
          className="flex-1 rounded-md border border-stone-300 bg-white px-3 py-2"
        />
        <button type="submit" className="rounded-md bg-stone-900 px-4 py-2 font-medium text-white hover:bg-stone-700">
          Rechercher
        </button>
      </form>

      {customers.isError && <ErrorMessage error={customers.error} />}
      <div className={`overflow-x-auto rounded-lg border border-stone-200 bg-white transition-opacity ${customers.isPlaceholderData ? 'opacity-60' : ''}`}>
        <table className="w-full text-left text-sm">
          <thead className="border-b border-stone-200 bg-stone-50 text-stone-600">
            <tr>
              <th scope="col" className="px-4 py-3 font-medium">Client</th>
              <th scope="col" className="px-4 py-3 font-medium">Inscrit le</th>
              <th scope="col" className="px-4 py-3 text-right font-medium">Commandes</th>
              <th scope="col" className="px-4 py-3 text-right font-medium">Montant commandé</th>
              <th scope="col" className="px-4 py-3 font-medium">Dernière commande</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {customers.data?.content.map((customer) => (
              <tr key={customer.id} className="hover:bg-stone-50">
                <td className="px-4 py-3">
                  <p className="font-medium text-stone-900">{customer.fullName}</p>
                  <p className="text-xs text-stone-500">{customer.email}</p>
                </td>
                <td className="px-4 py-3 text-stone-700">{formatDate(customer.createdAt)}</td>
                <td className="px-4 py-3 text-right tabular-nums">
                  {customer.orderCount > 0 ? (
                    <Link to={`/admin/commandes?q=${encodeURIComponent(customer.email)}`} className="font-semibold text-brand-700 hover:underline">
                      {customer.orderCount}
                      <span className="sr-only"> commandes de {customer.fullName}</span>
                    </Link>
                  ) : (
                    0
                  )}
                </td>
                <td className="px-4 py-3 text-right font-semibold tabular-nums">{formatPrice(customer.totalSpent)}</td>
                <td className="px-4 py-3 text-stone-700">{customer.lastOrderAt ? formatDate(customer.lastOrderAt) : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {customers.data?.content.length === 0 && <p className="p-8 text-center text-stone-600">Aucun client ne correspond.</p>}
      </div>
      <p className="mt-2 text-xs text-stone-500">Commandes et montants : commandes annulées exclues.</p>
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
