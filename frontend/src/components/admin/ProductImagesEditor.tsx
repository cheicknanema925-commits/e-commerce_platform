import { type ChangeEvent, type DragEvent, useState } from 'react'
import type { Product } from '../../api/types.ts'
import { useDeleteProductImage, useMakeMainProductImage, useUploadProductImage } from '../../hooks/useAdmin.ts'
import { ACCEPTED_IMAGE_TYPES, imageFileError, MAX_IMAGES_PER_PRODUCT } from '../../lib/images.ts'
import { ErrorMessage } from '../ErrorMessage.tsx'

// Photos d'un produit : ajout (bouton ou glisser-déposer), choix de la principale, suppression.
// Les fichiers partent un par un : une erreur sur l'un n'empêche pas l'envoi des autres.
export function ProductImagesEditor({ product }: { product: Product }) {
  const upload = useUploadProductImage()
  const makeMain = useMakeMainProductImage()
  const remove = useDeleteProductImage()
  const [problems, setProblems] = useState<string[]>([])
  const [sending, setSending] = useState(0)
  const [dragging, setDragging] = useState(false)
  const remaining = MAX_IMAGES_PER_PRODUCT - product.images.length

  async function send(files: File[]) {
    const errors: string[] = []
    if (files.length > remaining) {
      errors.push(`Seules ${remaining} photo(s) de plus sont possibles (${MAX_IMAGES_PER_PRODUCT} au maximum).`)
    }
    const valid = files.slice(0, Math.max(0, remaining)).filter((file) => {
      const error = imageFileError(file)
      if (error) {
        errors.push(error)
      }
      return error === null
    })
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

  function onPick(event: ChangeEvent<HTMLInputElement>) {
    send(Array.from(event.target.files ?? []))
    // Permet de choisir à nouveau le même fichier
    event.target.value = ''
  }

  function onDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    setDragging(false)
    send(Array.from(event.dataTransfer.files))
  }

  return (
    <section aria-labelledby="photos" className="rounded-lg border border-stone-200 bg-white p-6">
      <div className="flex items-baseline justify-between gap-2">
        <h2 id="photos" className="font-semibold">
          Photos
        </h2>
        <p className="text-sm text-stone-500">
          {product.images.length} / {MAX_IMAGES_PER_PRODUCT}
        </p>
      </div>
      <p className="mt-1 text-sm text-stone-600">La première photo est celle du catalogue. JPEG ou PNG, 5 Mo maximum.</p>

      {product.images.length > 0 && (
        <ul className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
          {product.images.map((image, index) => (
            <li key={image.id} className="overflow-hidden rounded-md border border-stone-200">
              <div className="relative">
                <img src={image.url} alt={`Photo ${index + 1} de ${product.name}`} className="aspect-square w-full object-cover" />
                {index === 0 && (
                  <span className="absolute left-2 top-2 rounded bg-stone-900/80 px-2 py-0.5 text-xs font-medium text-white">Principale</span>
                )}
              </div>
              <div className="flex flex-col gap-1 p-2 text-xs">
                {index > 0 && (
                  <button
                    type="button"
                    disabled={makeMain.isPending}
                    onClick={() => makeMain.mutate({ productId: product.id, imageId: image.id })}
                    className="text-left font-medium text-brand-700 hover:underline"
                  >
                    Mettre en principale<span className="sr-only"> (photo {index + 1})</span>
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
                  className="text-left font-medium text-red-700 hover:underline"
                >
                  Supprimer<span className="sr-only"> la photo {index + 1}</span>
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {remaining > 0 && (
        <label
          onDragOver={(event) => {
            event.preventDefault()
            setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
          className={`mt-4 flex cursor-pointer flex-col items-center justify-center rounded-md border-2 border-dashed px-4 py-8 text-center text-sm focus-within:outline-2 focus-within:outline-brand-700 ${
            dragging ? 'border-brand-600 bg-brand-50' : 'border-stone-300 hover:bg-stone-50'
          }`}
        >
          <span className="font-medium text-brand-700">Ajouter des photos</span>
          <span className="mt-1 text-stone-500">ou glissez-les ici</span>
          <input type="file" accept={ACCEPTED_IMAGE_TYPES.join(',')} multiple onChange={onPick} className="sr-only" />
        </label>
      )}

      <div aria-live="polite" className="mt-3 space-y-2">
        {sending > 0 && <p className="text-sm text-stone-600">Envoi en cours… ({sending} restante{sending > 1 ? 's' : ''})</p>}
        {problems.length > 0 && (
          <ul role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800">
            {problems.map((problem) => (
              <li key={problem}>{problem}</li>
            ))}
          </ul>
        )}
        {makeMain.isError && <ErrorMessage error={makeMain.error} />}
        {remove.isError && <ErrorMessage error={remove.error} />}
      </div>
    </section>
  )
}
