import { useState } from 'react'
import type { Product } from '../../api/types.ts'
import { useDeleteProductImage, useMakeMainProductImage, useUploadProductImage } from '../../hooks/useAdmin.ts'
import { MAX_IMAGES_PER_PRODUCT, selectImages } from '../../lib/images.ts'
import { ErrorMessage } from '../ErrorMessage.tsx'
import { ImageDropZone } from './ImageDropZone.tsx'
import { btnMini, btnMiniDanger, Panel } from './ui.tsx'

// Photos d'un produit existant : ajout (bouton ou glisser-déposer), choix de la principale, suppression.
// Les fichiers partent un par un : une erreur sur l'un n'empêche pas l'envoi des autres.
export function ProductImagesEditor({ product }: { product: Product }) {
  const upload = useUploadProductImage()
  const makeMain = useMakeMainProductImage()
  const remove = useDeleteProductImage()
  const [problems, setProblems] = useState<string[]>([])
  const [sending, setSending] = useState(0)
  const remaining = MAX_IMAGES_PER_PRODUCT - product.images.length

  async function send(files: File[]) {
    const { valid, errors } = selectImages(files, remaining)
    setSending(valid.length)
    for (const file of valid) {
      try {
        await upload.mutateAsync({ productId: product.id, file })
      } catch (error) {
        errors.push(`${file.name} : ${error instanceof Error ? error.message : 'envoi impossible'}`)
      }
      setSending((count) => count - 1)
    }
    setProblems(errors)
  }

  return (
    <Panel title="Visuel du produit" action={<span className="text-sm text-stone-600">{product.images.length} / {MAX_IMAGES_PER_PRODUCT}</span>}>
      <p className="-mt-2 mb-4 text-sm text-stone-600">La première photo est celle du catalogue. Le serveur la recadre et la réduit automatiquement.</p>
      {product.images.length > 0 && (
        <ul className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-2">
          {product.images.map((image, index) => (
            <li key={image.id} className="overflow-hidden rounded-lg border border-stone-200">
              <div className="relative">
                <img src={image.url} alt={`Photo ${index + 1} de ${product.name}`} className="aspect-square w-full bg-stone-100 object-cover" />
                {index === 0 && <span className="absolute top-2 left-2 rounded-sm bg-safran px-2 py-0.5 text-xs font-bold text-stone-900">Principale</span>}
              </div>
              <div className="flex flex-wrap gap-1.5 p-2">
                {index > 0 && (
                  <button type="button" disabled={makeMain.isPending} onClick={() => makeMain.mutate({ productId: product.id, imageId: image.id })} className={btnMini}>
                    Principale<span className="sr-only"> : photo {index + 1}</span>
                  </button>
                )}
                <button
                  type="button"
                  disabled={remove.isPending}
                  onClick={() => {
                    if (window.confirm('Supprimer cette photo ?')) {
                      remove.mutate({ productId: product.id, imageId: image.id })
                    }
                  }}
                  className={btnMiniDanger}
                >
                  Supprimer<span className="sr-only"> la photo {index + 1}</span>
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
      {remaining > 0 ? <ImageDropZone onFiles={(files) => send(files)} disabled={sending > 0} /> : <p className="text-sm text-stone-600">Nombre maximum de photos atteint.</p>}
      <div aria-live="polite" className="mt-3 space-y-2">
        {sending > 0 && <p className="text-sm text-stone-600">Envoi en cours… ({sending} restante{sending > 1 ? 's' : ''})</p>}
        {problems.length > 0 && (
          <ul role="alert" className="rounded-lg border border-bissap/30 bg-bissap-soft p-3 text-sm text-bissap">
            {problems.map((problem) => (
              <li key={problem}>{problem}</li>
            ))}
          </ul>
        )}
        {makeMain.isError && <ErrorMessage error={makeMain.error} />}
        {remove.isError && <ErrorMessage error={remove.error} />}
      </div>
    </Panel>
  )
}
