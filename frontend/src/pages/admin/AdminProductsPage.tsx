import { type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ApiError } from '../../api/client.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { useDeleteProduct } from '../../hooks/useAdmin.ts'
import { useProducts } from '../../hooks/useCatalog.ts'
import { formatPrice } from '../../lib/format.ts'

export function AdminProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? undefined
  const page = Number(searchParams.get('page') ?? 0)
  const products = useProducts({ q, page, sort: 'name,asc' })
  const deleteProduct = useDeleteProduct()

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const value = String(new FormData(event.currentTarget).get('q') ?? '').trim()
    setSearchParams(value ? { q: value } : {})
  }

  return (
    <>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <form role="search" onSubmit={onSearch} className="flex gap-2">
          <label htmlFor="admin-q" className="sr-only">
            Rechercher un produit
          </label>
          <input id="admin-q" name="q" type="search" defaultValue={q} placeholder="Rechercher…" className="rounded-md border border-stone-300 px-3 py-2" />
          <button type="submit" className="rounded-md border border-stone-300 px-3 py-2 font-medium hover:bg-stone-100">
            Rechercher
          </button>
        </form>
        <Link to="/admin/produits/nouveau" className="rounded-md bg-brand-700 px-4 py-2 font-medium text-white hover:bg-brand-800">
          Nouveau produit
        </Link>
      </div>

      <div aria-live="polite" className="mt-4">
        {deleteProduct.error instanceof ApiError && deleteProduct.error.status === 409 ? (
          // Les commandes passées gardent une référence au produit (clé étrangère) : l'historique doit rester intact
          <p role="alert" className="rounded-md border border-amber-200 bg-amber-50 p-4 text-amber-900">
            Ce produit a déjà été commandé, mis dans un panier ou noté : il ne peut pas être supprimé. Pour le retirer de
            la vente, mettez son stock à 0.
          </p>
        ) : (
          deleteProduct.isError && <ErrorMessage error={deleteProduct.error} />
        )}
        {products.isError && <ErrorMessage error={products.error} />}
      </div>

      <div className="mt-4 overflow-x-auto rounded-lg border border-stone-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-stone-100 text-stone-700">
            <tr>
              <th scope="col" className="px-4 py-3">Produit</th>
              <th scope="col" className="px-4 py-3">Catégorie</th>
              <th scope="col" className="px-4 py-3 text-right">Prix</th>
              <th scope="col" className="px-4 py-3 text-right">Stock</th>
              <th scope="col" className="px-4 py-3"><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-200">
            {products.data?.content.map((product) => (
              <tr key={product.id}>
                <td className="px-4 py-3 font-medium">{product.name}</td>
                <td className="px-4 py-3 text-stone-600">{product.categoryName}</td>
                <td className="px-4 py-3 text-right">{formatPrice(product.price)}</td>
                <td className={`px-4 py-3 text-right ${product.stock <= 5 ? 'font-semibold text-amber-700' : ''}`}>{product.stock}</td>
                <td className="px-4 py-3 text-right whitespace-nowrap">
                  <Link to={`/admin/produits/${product.id}`} className="font-medium text-brand-700 hover:underline">
                    Modifier<span className="sr-only"> {product.name}</span>
                  </Link>
                  <button
                    type="button"
                    disabled={deleteProduct.isPending}
                    onClick={() => {
                      if (window.confirm(`Supprimer « ${product.name} » ?`)) {
                        deleteProduct.mutate(product.id)
                      }
                    }}
                    className="ml-4 font-medium text-red-700 hover:underline"
                  >
                    Supprimer<span className="sr-only"> {product.name}</span>
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {products.data && (
        <Pagination
          page={products.data.page}
          totalPages={products.data.totalPages}
          onChange={(next) => setSearchParams({ ...(q ? { q } : {}), page: String(next) })}
        />
      )}
    </>
  )
}
