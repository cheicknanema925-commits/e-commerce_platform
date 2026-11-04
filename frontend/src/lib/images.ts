// Contrôle immédiat AVANT l'envoi (confort : message sans attendre le réseau).
// Le serveur refait toutes les vérifications : un navigateur modifié peut contourner celles-ci.
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024

export const MAX_IMAGES_PER_PRODUCT = 8

export const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png']

export function imageFileError(file: File): string | null {
  if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
    return `${file.name} : formats acceptés JPEG ou PNG.`
  }
  if (file.size > MAX_IMAGE_BYTES) {
    return `${file.name} : 5 Mo maximum.`
  }
  return null
}

// Tri des fichiers choisis : ceux qu'on peut envoyer, et un message pour chacun des autres
export function selectImages(files: File[], remaining: number): { valid: File[]; errors: string[] } {
  const errors: string[] = []
  if (files.length > remaining) {
    errors.push(`Seulement ${Math.max(0, remaining)} photo(s) de plus possible(s) : ${MAX_IMAGES_PER_PRODUCT} au maximum par produit.`)
  }
  const valid = files.slice(0, Math.max(0, remaining)).filter((file) => {
    const error = imageFileError(file)
    if (error) {
      errors.push(error)
    }
    return error === null
  })
  return { valid, errors }
}
