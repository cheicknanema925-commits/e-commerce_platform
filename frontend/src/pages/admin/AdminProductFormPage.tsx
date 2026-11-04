import { type FormEvent, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { FormField, inputClass } from '../../components/FormField.tsx'
import { type PendingImage, PendingImages } from '../../components/admin/PendingImages.tsx'
import { ProductImagesEditor } from '../../components/admin/ProductImagesEditor.tsx'
import { btnGhost, btnPrimary, PageHeader, Panel } from '../../components/admin/ui.tsx'
import { useCreateProduct, useUpdateProduct, useUploadProductImage } from '../../hooks/useAdmin.ts'
import { useCategories, useProduct } from '../../hooks/useCatalog.ts'
import { fieldErrors } from '../../lib/forms.ts'
import { MAX_IMAGES_PER_PRODUCT, selectImages } from '../../lib/images.ts'
import { useToast } from '../../lib/toast.ts'

// Après la création, la page de modification affiche les éventuels échecs d'envoi des photos
type CreatedState = { created?: boolean; uploadErrors?: string[] }

// Même page pour créer (/admin/produits/nouveau) et modifier (/admin/produits/:id)
export function AdminProductFormPage() {
  const params = useParams()
  const location = useLocation()
  const id = params.id === undefined ? null : Number(params.id)
  const product = useProduct(id ?? Number.NaN)
  const categories = useCategories()
  const create = useCreateProduct()
  const update = useUpdateProduct()
  const upload = useUploadProductImage()
  const navigate = useNavigate()
  const toast = useToast()
  const [pending, setPending] = useState<PendingImage[]>([])
  const [pendingProblems, setPendingProblems] = useState<string[]>([])
  const [uploading, setUploading] = useState(false)
  const mutation = id === null ? create : update
  const errors = fieldErrors(mutation.error)
  const created = (location.state as CreatedState | null) ?? {}

  if (product.isError || categories.isError) {
    return <ErrorMessage error={product.error ?? categories.error} />
  }
  // defaultValue n'est lu qu'au premier affichage : on attend le produit ET les catégories avant de rendre le formulaire
  if ((id !== null && !product.data) || !categories.data) {
    return <p className="text-stone-600">Chargement…</p>
  }

  function addPending(files: File[]) {
    const { valid, errors: problems } = selectImages(files, MAX_IMAGES_PER_PRODUCT - pending.length)
    setPending((current) => [...current, ...valid.map((file) => ({ file, preview: URL.createObjectURL(file) }))])
    setPendingProblems(problems)
  }

  function removePending(index: number) {
    URL.revokeObjectURL(pending[index].preview)
    setPending((current) => current.filter((_, i) => i !== index))
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const input = {
      categoryId: Number(form.get('categoryId')),
      name: String(form.get('name')).trim(),
      description: String(form.get('description') ?? '').trim() || null,
      price: Number(form.get('price')),
      stock: Number(form.get('stock')),
      brand: String(form.get('brand') ?? '').trim() || null,
      sku: String(form.get('sku') ?? '').trim() || null,
      // Vide = pas de promotion
      compareAtPrice: form.get('compareAtPrice') ? Number(form.get('compareAtPrice')) : null,
    }
    if (id !== null) {
      await update.mutateAsync({ id, product: input })
      toast('Modifications enregistrées')
      return
    }
    const product = await create.mutateAsync(input)
    // Les photos partent une par une, maintenant que le produit a un id
    setUploading(true)
    const uploadErrors: string[] = []
    for (const image of pending) {
      try {
        await upload.mutateAsync({ productId: product.id, file: image.file })
      } catch (error) {
        uploadErrors.push(`${image.file.name} : ${error instanceof Error ? error.message : 'envoi impossible'}`)
      }
    }
    toast(`Produit enregistré : ${product.name}`)
    navigate(`/admin/produits/${product.id}`, { replace: true, state: { created: true, uploadErrors } satisfies CreatedState })
  }

  const current = product.data
  const busy = mutation.isPending || uploading
  return (
    <>
      <PageHeader
        title={current ? 'Modifier un article' : 'Ajouter un article'}
        description={current ? current.name : 'Renseignez les informations du produit à mettre en vente.'}
        actions={
          <>
            {current && (
              <Link to={`/produits/${current.id}`} className={btnGhost}>
                Voir dans la boutique
              </Link>
            )}
            <Link to="/admin/produits" className={btnGhost}>
              Retour au catalogue
            </Link>
          </>
        }
      />

      {created.created && id !== null && (
        <div aria-live="polite" className="mb-4 space-y-2">
          <p className="rounded-lg bg-kola-soft p-4 text-sm font-medium text-kola">Produit créé et mis en vente.</p>
          {created.uploadErrors && created.uploadErrors.length > 0 && (
            <ul role="alert" className="rounded-lg border border-bissap/30 bg-bissap-soft p-3 text-sm text-bissap">
              {created.uploadErrors.map((problem) => (
                <li key={problem}>{problem}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      <div className="grid items-start gap-4 xl:grid-cols-[1.3fr_0.7fr]">
        <form key={id ?? 'nouveau'} id="formulaire-produit" onSubmit={(event) => onSubmit(event).catch(() => setUploading(false))}>
          <Panel title="Informations générales">
            <div className="flex flex-col gap-3.5">
              {mutation.isError && Object.keys(errors).length === 0 && <ErrorMessage error={mutation.error} />}
              <FormField name="name" label="Nom du produit *" required maxLength={100} defaultValue={current?.name} placeholder="Ex. Boubou en bazin brodé" error={errors.name} />
              <div className="grid gap-3 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <label htmlFor="categoryId" className="text-sm font-semibold">
                  Catégorie *
                </label>
                <select id="categoryId" name="categoryId" required defaultValue={current?.categoryId ?? ''} className={`${inputClass} border-stone-200`}>
                  <option value="" disabled>
                    Sélectionner
                  </option>
                  {categories.data.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </div>
              <FormField name="brand" label="Marque" maxLength={80} defaultValue={current?.brand ?? ''} placeholder="Ex. Vlisco, Samsung" error={errors.brand} />
              </div>
              <div className="flex flex-col gap-1.5">
                <label htmlFor="description" className="text-sm font-semibold">
                  Description du produit
                </label>
                <textarea
                  id="description"
                  name="description"
                  rows={5}
                  maxLength={1000}
                  defaultValue={current?.description ?? ''}
                  placeholder="Caractéristiques, dimensions, matière, garantie…"
                  className={`${inputClass} resize-y border-stone-200`}
                />
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <FormField name="price" label="Prix de vente (FCFA) *" type="number" required min={1} step={1} defaultValue={current?.price} placeholder="Ex. 25000" error={errors.price} />
                <FormField
                  name="stock"
                  label="Quantité en stock *"
                  type="number"
                  required
                  min={0}
                  step={1}
                  defaultValue={current?.stock ?? ''}
                  placeholder="Ex. 25"
                  hint="0 = rupture : le produit reste visible mais ne peut plus être commandé."
                  error={errors.stock}
                />
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <FormField
                  name="compareAtPrice"
                  label="Prix barré (FCFA)"
                  type="number"
                  min={1}
                  step={1}
                  defaultValue={current?.compareAtPrice ?? ''}
                  hint="Facultatif : prix avant remise, plus élevé que le prix de vente. Le produit apparaît dans « Promotions »."
                  error={errors.compareAtPrice}
                />
                <FormField
                  name="sku"
                  label="Référence"
                  maxLength={40}
                  pattern="[A-Za-z0-9._\-]{1,40}"
                  defaultValue={current?.sku ?? ''}
                  placeholder="Ex. SHA-007"
                  hint="Code interne unique. Générée automatiquement si vide."
                  error={errors.sku}
                />
              </div>
              <p className="text-[13px] text-stone-600">Les champs marqués d’un * sont obligatoires.</p>
            </div>
          </Panel>
        </form>

        <div className="flex flex-col gap-4">
          {current ? (
            <ProductImagesEditor product={current} />
          ) : (
            <PendingImages images={pending} onAdd={addPending} onRemove={removePending} problems={pendingProblems} />
          )}
        </div>
      </div>

      <div className="mt-4 flex justify-end gap-2.5">
        <Link to="/admin/produits" className={btnGhost}>
          Annuler
        </Link>
        <button type="submit" form="formulaire-produit" disabled={busy} className={btnPrimary}>
          {uploading ? 'Envoi des photos…' : mutation.isPending ? 'Enregistrement…' : 'Enregistrer le produit'}
        </button>
      </div>
    </>
  )
}
