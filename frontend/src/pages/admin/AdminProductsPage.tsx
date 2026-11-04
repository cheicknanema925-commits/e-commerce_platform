import { type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ApiError } from '../../api/client.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { Icon } from '../../components/Icon.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { ProductThumbnail } from '../../components/ProductThumbnail.tsx'
import { btnMini, btnMiniDanger, btnPrimary, filterInput, PageHeader, TableCard, td, th } from '../../components/admin/ui.tsx'
import { useDeleteProduct } from '../../hooks/useAdmin.ts'
import { useCategories, useProducts } from '../../hooks/useCatalog.ts'
import { formatPrice } from '../../lib/format.ts'
import { useToast } from '../../lib/toast.ts'

function ProductStatus({ stock }: { stock: number }) {
  const [label, style] =
    stock === 0 ? ['Rupture', 'bg-bissap-soft text-bissap'] : stock <= 5 ? ['Stock faible', 'bg-safran-soft text-safran-ink'] : ['En vente', 'bg-kola-soft text-kola']
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-[13px] font-semibold before:size-[7px] before:rounded-full before:bg-current ${style}`}>
      {label}
    </span>
  )
}

export function AdminProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? undefined
  const category = searchParams.get('categorie') ?? undefined
  const page = Number(searchParams.get('page') ?? 0)
  const products = useProducts({ q, category, page, sort: 'name,asc' })
  const categories = useCategories()
  const deleteProduct = useDeleteProduct()
  const toast = useToast()

  function update(name: string, value: string) {
    const next = new URLSearchParams(searchParams)
    if (value) {
      next.set(name, value)
    } else {
      next.delete(name)
    }
    next.delete('page')
    setSearchParams(next)
  }

  function onSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    update('q', String(new FormData(event.currentTarget).get('q') ?? '').trim())
  }

  return (
    <>
      <PageHeader
        title="Gestion des produits"
        description="Consultez, modifiez et gérez votre catalogue."
        actions={
          <Link to="/admin/produits/nouveau" className={btnPrimary}>
            <Icon name="plus" className="size-4" />
            Ajouter un article
          </Link>
        }
      />

      <div className="flex flex-wrap gap-2.5">
        <form role="search" onSubmit={onSearch} className="flex flex-1 basis-full sm:flex-none sm:basis-auto">
          <label htmlFor="recherche-produit" className="sr-only">
            Rechercher un produit
          </label>
          <input id="recherche-produit" name="q" type="search" key={q} defaultValue={q} maxLength={100} placeholder="Rechercher par nom" className={`${filterInput} w-full sm:w-72`} />
        </form>
        <select aria-label="Catégorie" value={category ?? ''} onChange={(event) => update('categorie', event.target.value)} className={filterInput}>
          <option value="">Toutes les catégories</option>
          {categories.data?.map((c) => (
            <option key={c.id} value={c.slug}>
              {c.name}
            </option>
          ))}
        </select>
      </div>

      <div aria-live="polite">
        {deleteProduct.error instanceof ApiError && deleteProduct.error.status === 409 ? (
          // Les commandes passées gardent une référence au produit (clé étrangère) : l'historique doit rester intact
          <p role="alert" className="mt-4 rounded-lg border border-safran/50 bg-safran-soft p-4 text-sm text-safran-ink">
            Ce produit a déjà été commandé, mis dans un panier ou noté : il ne peut pas être supprimé. Pour le retirer de la
            vente, mettez son stock à 0.
          </p>
        ) : (
          deleteProduct.isError && (
            <div className="mt-4">
              <ErrorMessage error={deleteProduct.error} />
            </div>
          )
        )}
        {products.isError && <ErrorMessage error={products.error} />}
      </div>

      <TableCard title="Catalogue" count={products.data?.totalElements}>
        <table className={`w-full text-sm transition-opacity ${products.isPlaceholderData ? 'opacity-60' : ''}`}>
          <thead>
            <tr>
              <th scope="col" className={th}>Produit</th>
              <th scope="col" className={th}>Catégorie</th>
              <th scope="col" className={`${th} text-right`}>Prix</th>
              <th scope="col" className={`${th} text-right`}>Stock</th>
              <th scope="col" className={th}>Photos</th>
              <th scope="col" className={th}>Statut</th>
              <th scope="col" className={th}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {products.data?.content.map((product) => (
              <tr key={product.id} className="hover:bg-stone-50">
                <td className={td}>
                  <div className="flex items-center gap-3">
                    <ProductThumbnail product={product} className="size-10 shrink-0 rounded-lg text-xl" />
                    <b className="font-semibold">{product.name}</b>
                  </div>
                </td>
                <td className={td}>{product.categoryName}</td>
                <td className={`${td} text-right font-semibold tabular-nums`}>{formatPrice(product.price)}</td>
                <td className={`${td} text-right tabular-nums ${product.stock < 10 ? 'font-semibold text-bissap' : ''}`}>{product.stock}</td>
                <td className={td}>{product.images.length === 0 ? <span className="font-medium text-safran-ink">Aucune</span> : `${product.images.length} / 8`}</td>
                <td className={td}>
                  <ProductStatus stock={product.stock} />
                </td>
                <td className={td}>
                  <div className="flex gap-1.5">
                    <Link to={`/admin/produits/${product.id}`} className={btnMini}>
                      Modifier<span className="sr-only"> {product.name}</span>
                    </Link>
                    <button
                      type="button"
                      disabled={deleteProduct.isPending}
                      onClick={() => {
                        if (window.confirm(`Supprimer « ${product.name} » et ses photos ?`)) {
                          deleteProduct.mutate(product.id, { onSuccess: () => toast(`Produit supprimé : ${product.name}`) })
                        }
                      }}
                      className={btnMiniDanger}
                    >
                      Supprimer<span className="sr-only"> {product.name}</span>
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {products.data?.content.length === 0 && <p className="p-9 text-center text-stone-600">Aucun produit ne correspond à ces filtres.</p>}
      </TableCard>
      {products.data && (
        <Pagination
          page={products.data.page}
          totalPages={products.data.totalPages}
          onChange={(next) => {
            const params = new URLSearchParams(searchParams)
            params.set('page', String(next))
            setSearchParams(params)
          }}
        />
      )}
    </>
  )
}
