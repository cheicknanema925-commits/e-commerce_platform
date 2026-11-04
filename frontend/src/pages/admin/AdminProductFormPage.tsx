import { type FormEvent } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { FormField } from '../../components/FormField.tsx'
import { PageHeader } from '../../components/admin/PageHeader.tsx'
import { ProductImagesEditor } from '../../components/admin/ProductImagesEditor.tsx'
import { useCreateProduct, useUpdateProduct } from '../../hooks/useAdmin.ts'
import { useCategories, useProduct } from '../../hooks/useCatalog.ts'
import { fieldErrors } from '../../lib/forms.ts'

// Même page pour créer (/admin/produits/nouveau) et modifier (/admin/produits/:id).
// Les photos ont besoin d'un produit existant : après la création, on arrive sur la modification pour les ajouter.
export function AdminProductFormPage() {
  const params = useParams()
  const [searchParams] = useSearchParams()
  const id = params.id === undefined ? null : Number(params.id)
  const product = useProduct(id ?? Number.NaN)
  const categories = useCategories()
  const create = useCreateProduct()
  const update = useUpdateProduct()
  const navigate = useNavigate()
  const mutation = id === null ? create : update
  const errors = fieldErrors(mutation.error)

  if (product.isError || categories.isError) {
    return <ErrorMessage error={product.error ?? categories.error} />
  }
  // defaultValue n'est lu qu'au premier affichage : on attend le produit ET les catégories avant de rendre le formulaire
  if ((id !== null && !product.data) || !categories.data) {
    return <p className="text-stone-600">Chargement…</p>
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const input = {
      categoryId: Number(form.get('categoryId')),
      name: String(form.get('name')),
      description: String(form.get('description') ?? '') || null,
      price: Number(form.get('price')),
      stock: Number(form.get('stock')),
    }
    if (id === null) {
      const created = await create.mutateAsync(input)
      navigate(`/admin/produits/${created.id}?nouveau=1`, { replace: true })
    } else {
      await update.mutateAsync({ id, product: input })
    }
  }

  const current = product.data
  return (
    <>
      <Link to="/admin/produits" className="text-sm font-medium text-brand-700 hover:underline">
        ← Produits
      </Link>
      <div className="mt-2">
        <PageHeader
          title={current ? current.name : 'Nouveau produit'}
          actions={
            current && (
              <Link to={`/produits/${current.id}`} className="rounded-md border border-stone-300 bg-white px-4 py-2 text-sm font-medium hover:bg-stone-100">
                Voir dans la boutique
              </Link>
            )
          }
        />
      </div>

      <div aria-live="polite">
        {searchParams.get('nouveau') === '1' && !update.isSuccess && (
          <p className="mb-4 rounded-md border border-brand-200 bg-brand-50 p-4 text-brand-900">
            Produit créé. Ajoutez maintenant ses photos.
          </p>
        )}
        {update.isSuccess && (
          <p className="mb-4 rounded-md border border-brand-200 bg-brand-50 p-4 text-brand-900">Modifications enregistrées.</p>
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-5">
        <form
          key={id ?? 'nouveau'}
          onSubmit={(event) => onSubmit(event).catch(() => undefined)}
          className="flex flex-col gap-4 rounded-lg border border-stone-200 bg-white p-6 lg:col-span-3"
        >
          <h2 className="font-semibold">Informations</h2>
          {mutation.isError && Object.keys(errors).length === 0 && <ErrorMessage error={mutation.error} />}
          <FormField name="name" label="Nom" required maxLength={100} defaultValue={current?.name} error={errors.name} />
          <div className="flex flex-col gap-1">
            <label htmlFor="categoryId" className="text-sm font-medium text-stone-700">
              Catégorie
            </label>
            <select id="categoryId" name="categoryId" required defaultValue={current?.categoryId ?? ''} className="rounded-md border border-stone-300 bg-white px-3 py-2">
              <option value="" disabled>
                Choisir…
              </option>
              {categories.data.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="description" className="text-sm font-medium text-stone-700">
              Description
            </label>
            <textarea
              id="description"
              name="description"
              rows={6}
              maxLength={1000}
              defaultValue={current?.description ?? ''}
              className="rounded-md border border-stone-300 px-3 py-2"
            />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField name="price" label="Prix (F CFA)" type="number" required min={1} step={1} defaultValue={current?.price} error={errors.price} />
            <FormField
              name="stock"
              label="Stock"
              type="number"
              required
              min={0}
              step={1}
              defaultValue={current?.stock ?? 0}
              hint="0 = retiré de la vente (rupture)"
              error={errors.stock}
            />
          </div>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="self-start rounded-md bg-brand-700 px-5 py-2.5 font-medium text-white hover:bg-brand-800 disabled:opacity-60"
          >
            {mutation.isPending ? 'Enregistrement…' : id === null ? 'Créer le produit' : 'Enregistrer'}
          </button>
        </form>

        <div className="lg:col-span-2">
          {current ? (
            <ProductImagesEditor product={current} />
          ) : (
            <section className="rounded-lg border border-dashed border-stone-300 bg-white p-6 text-sm text-stone-600">
              <h2 className="font-semibold text-stone-900">Photos</h2>
              <p className="mt-1">Créez d'abord le produit : vous pourrez ensuite ajouter jusqu'à 8 photos.</p>
            </section>
          )}
        </div>
      </div>
    </>
  )
}
