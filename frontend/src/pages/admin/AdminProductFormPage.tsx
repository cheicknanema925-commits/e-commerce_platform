import { type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { FormField } from '../../components/FormField.tsx'
import { useCreateProduct, useUpdateProduct } from '../../hooks/useAdmin.ts'
import { useCategories, useProduct } from '../../hooks/useCatalog.ts'
import { fieldErrors } from '../../lib/forms.ts'

// Même formulaire pour créer (/admin/produits/nouveau) et modifier (/admin/produits/:id)
export function AdminProductFormPage() {
  const params = useParams()
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
      await create.mutateAsync(input)
    } else {
      await update.mutateAsync({ id, product: input })
    }
    navigate('/admin/produits')
  }

  const current = product.data
  return (
    <div className="max-w-2xl">
      <Link to="/admin/produits" className="text-sm font-medium text-brand-700 hover:underline">
        ← Produits
      </Link>
      <h2 className="mt-2 text-xl font-semibold">{id === null ? 'Nouveau produit' : `Modifier « ${current?.name} »`}</h2>

      <form onSubmit={(event) => onSubmit(event).catch(() => undefined)} className="mt-4 flex flex-col gap-4 rounded-lg border border-stone-200 bg-white p-6">
        {mutation.isError && Object.keys(errors).length === 0 && <ErrorMessage error={mutation.error} />}
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
        <FormField name="name" label="Nom" required maxLength={100} defaultValue={current?.name} error={errors.name} />
        <div className="flex flex-col gap-1">
          <label htmlFor="description" className="text-sm font-medium text-stone-700">
            Description
          </label>
          <textarea id="description" name="description" rows={4} maxLength={1000} defaultValue={current?.description ?? ''} className="rounded-md border border-stone-300 px-3 py-2" />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField name="price" label="Prix (F CFA)" type="number" required min={1} step={1} defaultValue={current?.price} error={errors.price} />
          <FormField name="stock" label="Stock" type="number" required min={0} step={1} defaultValue={current?.stock} error={errors.stock} />
        </div>
        <button type="submit" disabled={mutation.isPending} className="rounded-md bg-brand-700 px-4 py-2.5 font-medium text-white hover:bg-brand-800 disabled:opacity-60">
          {mutation.isPending ? 'Enregistrement…' : 'Enregistrer'}
        </button>
      </form>
    </div>
  )
}
