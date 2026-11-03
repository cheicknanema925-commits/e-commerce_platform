import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/client.ts'
import { fieldErrors } from './forms.ts'

describe('fieldErrors', () => {
  it('indexe les erreurs de validation de l’API par nom de champ', () => {
    const error = new ApiError(400, {
      status: 400,
      detail: 'La requête contient des champs invalides.',
      errors: [
        { field: 'price', message: 'doit être supérieur à 0' },
        { field: 'name', message: 'ne doit pas être vide' },
      ],
    })

    expect(fieldErrors(error)).toEqual({ price: 'doit être supérieur à 0', name: 'ne doit pas être vide' })
  })

  it('renvoie un objet vide pour une erreur sans détail par champ (409, panne réseau…)', () => {
    expect(fieldErrors(new ApiError(409, { status: 409, detail: 'Conflit' }))).toEqual({})
    expect(fieldErrors(new TypeError('Failed to fetch'))).toEqual({})
    expect(fieldErrors(null)).toEqual({})
  })
})
