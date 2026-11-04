import { type DragEvent, useState } from 'react'
import { ACCEPTED_IMAGE_TYPES } from '../../lib/images.ts'
import { Icon } from '../Icon.tsx'

// Zone « glissez une image ici ou cliquez pour la choisir » : un vrai <input type="file"> (clavier compris)
export function ImageDropZone({ onFiles, disabled = false }: { onFiles: (files: File[]) => void; disabled?: boolean }) {
  const [dragging, setDragging] = useState(false)

  function onDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    setDragging(false)
    if (!disabled) {
      onFiles(Array.from(event.dataTransfer.files))
    }
  }

  return (
    <label
      onDragOver={(event) => {
        event.preventDefault()
        setDragging(true)
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={onDrop}
      className={`block cursor-pointer rounded-lg border-2 border-dashed p-6.5 text-center text-sm text-stone-600 focus-within:outline-3 focus-within:outline-safran ${
        dragging ? 'border-brand-700 bg-brand-50' : 'border-stone-200 bg-stone-100 hover:border-stone-400'
      } ${disabled ? 'pointer-events-none opacity-60' : ''}`}
    >
      <Icon name="image" className="mx-auto mb-2 size-8 text-brand-700" />
      Glissez des photos ici ou <span className="font-semibold text-brand-700 underline">cliquez pour les choisir</span>
      <small className="mt-1 block">JPEG ou PNG, 5 Mo maximum par photo, 8 photos par produit</small>
      <input
        type="file"
        accept={ACCEPTED_IMAGE_TYPES.join(',')}
        multiple
        disabled={disabled}
        onChange={(event) => {
          onFiles(Array.from(event.target.files ?? []))
          // Permet de choisir à nouveau le même fichier
          event.target.value = ''
        }}
        className="sr-only"
      />
    </label>
  )
}
