import { describe, expect, it } from 'vitest'
import { imageFileError, MAX_IMAGE_BYTES } from './images.ts'

describe('imageFileError', () => {
  it('accepte une photo JPEG ou PNG de taille raisonnable', () => {
    expect(imageFileError(new File(['x'], 'boubou.jpg', { type: 'image/jpeg' }))).toBeNull()
    expect(imageFileError(new File(['x'], 'boubou.png', { type: 'image/png' }))).toBeNull()
  })

  it('refuse les autres formats, dont le SVG qui peut contenir du JavaScript', () => {
    expect(imageFileError(new File(['<svg/>'], 'logo.svg', { type: 'image/svg+xml' }))).toBe(
      'logo.svg : formats acceptés JPEG ou PNG.',
    )
  })

  it('refuse un fichier de plus de 5 Mo', () => {
    const big = new File([new Uint8Array(MAX_IMAGE_BYTES + 1)], 'photo.jpg', { type: 'image/jpeg' })
    expect(imageFileError(big)).toBe('photo.jpg : 5 Mo maximum.')
  })
})
