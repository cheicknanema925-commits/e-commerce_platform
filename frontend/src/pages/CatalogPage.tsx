import { type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import type { ProductSort } from '../api/catalog.ts'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { Pagination } from '../components/Pagination.tsx'
import { ProductCard } from '../components/ProductCard.tsx'
import { useCategories, useProducts } from '../hooks/useCatalog.ts'

const SORTS: { value: ProductSort; label: string }[] = [
  { value: 'id,desc', label: 'Nouveautés' },
  { value: 'price,asc', label: 'Prix croissant' },
  { value: 'price,desc', label: 'Prix décroissant' },
  { value: 'name,asc', label: 'Nom (A → Z)' },
]

// Les filtres vivent dans l'URL (?q=bazin&category=mode&page=1) : on peut partager le lien,
// et les boutons Précédent / Suivant du navigateur fonctionnent
export function CatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = {
    q: searchParams.get('q') ?? undefined,
    category: searchParams.get('category') ?? undefined,
    sort: (searchParams.get('sort') as ProductSort | null) ?? undefined,
    page: Number(searchParams.get('page') ?? 0),
  }
  const products = useProducts(filters)
  const categories = useCategories()

  // Changer un filtre ramène à la première page
  function updateFilter(name: string, value: string) {
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
    updateFilter('q', String(new FormData(event.currentTarget).get('q') ?? '').trim())
  }

  function goToPage(page: number) {
    const next = new URLSearchParams(searchParams)
    next.set('page', String(page))
    setSearchParams(next)
    window.scrollTo({ top: 0 })
  }

  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight">Catalogue</h1>

      <div className="mt-6 flex flex-col gap-4 rounded-lg border border-stone-200 bg-white p-4 md:flex-row md:items-end">
        <form role="search" onSubmit={onSearch} className="flex flex-1 flex-col gap-1">
          <label htmlFor="q" className="text-sm font-medium text-stone-700">
            Rechercher un produit
          </label>
          <div className="flex gap-2">
            <input
              id="q"
              name="q"
              type="search"
              maxLength={100}
              defaultValue={filters.q}
              key={filters.q}
              placeholder="Ex. : bazin, sandales…"
              className="w-full rounded-md border border-stone-300 px-3 py-2"
            />
            <button type="submit" className="rounded-md bg-brand-700 px-4 py-2 font-medium text-white hover:bg-brand-800">
              Rechercher
            </button>
          </div>
        </form>

        <div className="flex flex-col gap-1">
          <label htmlFor="category" className="text-sm font-medium text-stone-700">
            Catégorie
          </label>
          <select
            id="category"
            value={filters.category ?? ''}
            onChange={(event) => updateFilter('category', event.target.value)}
            className="rounded-md border border-stone-300 bg-white px-3 py-2"
          >
            <option value="">Toutes</option>
            {categories.data?.map((category) => (
              <option key={category.id} value={category.slug}>
                {category.name}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="sort" className="text-sm font-medium text-stone-700">
            Trier par
          </label>
          <select
            id="sort"
            value={filters.sort ?? 'id,desc'}
            onChange={(event) => updateFilter('sort', event.target.value)}
            className="rounded-md border border-stone-300 bg-white px-3 py-2"
          >
            {SORTS.map((sort) => (
              <option key={sort.value} value={sort.value}>
                {sort.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <section aria-labelledby="resultats" className="mt-8">
        <h2 id="resultats" className="sr-only">
          Résultats
        </h2>
        {products.isPending && <p className="text-stone-600">Chargement du catalogue…</p>}
        {products.isError && <ErrorMessage error={products.error} />}
        {products.data && (
          <>
            {/* aria-live : le nombre de résultats est annoncé quand les filtres changent */}
            <p aria-live="polite" className="mb-4 text-sm text-stone-600">
              {products.data.totalElements} produit{products.data.totalElements > 1 ? 's' : ''}
            </p>
            {products.data.content.length === 0 ? (
              <p className="rounded-lg border border-dashed border-stone-300 p-8 text-center text-stone-600">
                Aucun produit ne correspond à votre recherche.
              </p>
            ) : (
              <ul className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                {products.data.content.map((product) => (
                  <li key={product.id}>
                    <ProductCard product={product} />
                  </li>
                ))}
              </ul>
            )}
            <Pagination page={products.data.page} totalPages={products.data.totalPages} onChange={goToPage} />
          </>
        )}
      </section>
    </>
  )
}
