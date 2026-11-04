import { type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ApiError } from '../../api/client.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { ProductThumbnail } from '../../components/ProductThumbnail.tsx'
import { PageHeader } from '../../components/admin/PageHeader.tsx'
import { useDeleteProduct, useStats } from '../../hooks/useAdmin.ts'
import { useProducts } from '../../hooks/useCatalog.ts'
import { formatPrice } from '../../lib/format.ts'

function StockBadge({ stock }: { stock: number }) {
  if (stock === 0) {
    return <span className="rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-semibold text-red-800">Rupture</span>
  }
  if (stock <= 5) {
    return <span className="rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-semibold text-amber-900">Faible : {stock}</span>
  }
  return <span className="tabular-nums text-stone-700">{stock}</span>
}

export function AdminProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? undefined
  const page = Number(searchParams.get('page') ?? 0)
  const products = useProducts({ q, page, sort: 'name,asc' })
  const stats = useStats()
  const deleteProduct = useDeleteProduct()

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const value = String(new FormData(event.currentTarget).get('q') ?? '').trim()
    setSearchParams(value ? { q: value } : {})
  }

  const catalog = stats.data?.catalog
  return (
    <>
      <PageHeader
        title="Produits"
        description={
          catalog &&
          `${catalog.productCount} produits · ${catalog.outOfStock} en rupture · stock valorisé à ${formatPrice(catalog.stockValue)}`
        }
        actions={
          <Link to="/admin/produits/nouveau" className="rounded-md bg-brand-700 px-4 py-2 font-medium text-white hover:bg-brand-800">
            Nouveau produit
          </Link>
        }
      />

      <form role="search" onSubmit={onSearch} className="mb-4 flex max-w-xl gap-2">
        <label htmlFor="recherche-produit" className="sr-only">
          Rechercher un produit
        </label>
        <input
          id="recherche-produit"
          name="q"
          type="search"
          key={q}
          defaultValue={q}
          maxLength={100}
          placeholder="Nom du produit"
          className="flex-1 rounded-md border border-stone-300 bg-white px-3 py-2"
        />
        <button type="submit" className="rounded-md bg-stone-900 px-4 py-2 font-medium text-white hover:bg-stone-700">
          Rechercher
        </button>
      </form>

      <div aria-live="polite">
        {deleteProduct.error instanceof ApiError && deleteProduct.error.status === 409 ? (
          // Les commandes passées gardent une référence au produit (clé étrangère) : l'historique doit rester intact
          <p role="alert" className="mb-4 rounded-md border border-amber-200 bg-amber-50 p-4 text-amber-900">
            Ce produit a déjà été commandé, mis dans un panier ou noté : il ne peut pas être supprimé. Pour le retirer de
            la vente, mettez son stock à 0.
          </p>
        ) : (
          deleteProduct.isError && <ErrorMessage error={deleteProduct.error} />
        )}
        {products.isError && <ErrorMessage error={products.error} />}
      </div>

      <div className={`overflow-x-auto rounded-lg border border-stone-200 bg-white transition-opacity ${products.isPlaceholderData ? 'opacity-60' : ''}`}>
        <table className="w-full text-left text-sm">
          <thead className="border-b border-stone-200 bg-stone-50 text-stone-600">
            <tr>
              <th scope="col" className="px-4 py-3 font-medium">Produit</th>
              <th scope="col" className="px-4 py-3 font-medium">Catégorie</th>
              <th scope="col" className="px-4 py-3 text-right font-medium">Prix</th>
              <th scope="col" className="px-4 py-3 font-medium">Stock</th>
              <th scope="col" className="px-4 py-3 font-medium">Photos</th>
              <th scope="col" className="px-4 py-3">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {products.data?.content.map((product) => (
              <tr key={product.id} className="hover:bg-stone-50">
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <ProductThumbnail product={product} className="size-12 shrink-0 rounded-md text-lg" />
                    <Link to={`/admin/produits/${product.id}`} className="font-medium text-stone-900 hover:underline">
                      {product.name}
                    </Link>
                  </div>
                </td>
                <td className="px-4 py-3 text-stone-600">{product.categoryName}</td>
                <td className="px-4 py-3 text-right tabular-nums">{formatPrice(product.price)}</td>
                <td className="px-4 py-3">
                  <StockBadge stock={product.stock} />
                </td>
                <td className="px-4 py-3 tabular-nums text-stone-600">
                  {product.images.length === 0 ? <span className="text-amber-700">Aucune</span> : product.images.length}
                </td>
                <td className="px-4 py-3 text-right whitespace-nowrap">
                  <Link to={`/admin/produits/${product.id}`} className="font-medium text-brand-700 hover:underline">
                    Modifier<span className="sr-only"> {product.name}</span>
                  </Link>
                  <button
                    type="button"
                    disabled={deleteProduct.isPending}
                    onClick={() => {
                      if (window.confirm(`Supprimer « ${product.name} » et ses photos ?`)) {
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
        {products.data?.content.length === 0 && <p className="p-8 text-center text-stone-600">Aucun produit ne correspond.</p>}
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
