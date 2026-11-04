import { useEffect, useRef } from 'react'
import { MAX_IMAGES_PER_PRODUCT } from '../../lib/images.ts'
import { ImageDropZone } from './ImageDropZone.tsx'
import { btnMiniDanger, Panel } from './ui.tsx'

export type PendingImage = { file: File; preview: string }

// Photos choisies AVANT la création du produit : aperçu immédiat dans le navigateur (URL.createObjectURL),
// envoi au serveur juste après l'enregistrement du produit (il faut son id)
export function PendingImages({ images, onAdd, onRemove, problems }: { images: PendingImage[]; onAdd: (files: File[]) => void; onRemove: (index: number) => void; problems: string[] }) {
  // Libère la mémoire des aperçus quand la page se ferme
  const latest = useRef(images)
  useEffect(() => {
    latest.current = images
  })
  useEffect(() => () => latest.current.forEach((image) => URL.revokeObjectURL(image.preview)), [])

  return (
    <Panel title="Visuel du produit" action={<span className="text-sm text-stone-600">{images.length} / {MAX_IMAGES_PER_PRODUCT}</span>}>
      <p className="-mt-2 mb-4 text-sm text-stone-600">Les photos seront envoyées à l'enregistrement. La première sera celle du catalogue.</p>
      {images.length > 0 && (
        <ul className="mb-4 grid grid-cols-3 gap-2.5">
          {images.map((image, index) => (
            <li key={image.preview} className="overflow-hidden rounded-lg border border-stone-200">
              <div className="relative">
                <img src={image.preview} alt={`Aperçu de ${image.file.name}`} className="aspect-square w-full object-cover" />
                {index === 0 && <span className="absolute top-1.5 left-1.5 rounded-sm bg-safran px-1.5 text-[11px] font-bold">Principale</span>}
              </div>
              <button type="button" onClick={() => onRemove(index)} className={`${btnMiniDanger} m-1.5`}>
                Retirer<span className="sr-only"> {image.file.name}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
      {images.length < MAX_IMAGES_PER_PRODUCT && <ImageDropZone onFiles={onAdd} />}
      {problems.length > 0 && (
        <ul role="alert" className="mt-3 rounded-lg border border-bissap/30 bg-bissap-soft p-3 text-sm text-bissap">
          {problems.map((problem) => (
            <li key={problem}>{problem}</li>
          ))}
        </ul>
      )}
    </Panel>
  )
}
