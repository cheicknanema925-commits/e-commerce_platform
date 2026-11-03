import { describe, expect, it } from 'vitest'
import { ApiError, readResponse } from './client.ts'

describe('readResponse', () => {
  it('renvoie le JSON quand la réponse est un succès', async () => {
    const response = Response.json({ id: 1, name: 'Boubou bazin' })

    await expect(readResponse(response)).resolves.toEqual({ id: 1, name: 'Boubou bazin' })
  })

  it('transforme un ProblemDetail en ApiError avec le message de l’API', async () => {
    const response = new Response(JSON.stringify({ status: 404, title: 'Ressource introuvable', detail: 'Produit introuvable : 99' }), {
      status: 404,
      headers: { 'Content-Type': 'application/problem+json' },
    })

    const error = await readResponse(response).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(404)
    expect((error as ApiError).message).toBe('Produit introuvable : 99')
  })

  it('reste lisible quand le serveur ne renvoie pas de JSON (proxy en panne…)', async () => {
    const error = await readResponse(new Response('Bad Gateway', { status: 502 })).catch((e: unknown) => e)

    expect((error as ApiError).message).toBe('Erreur 502')
    expect((error as ApiError).problem).toBeNull()
  })
})
