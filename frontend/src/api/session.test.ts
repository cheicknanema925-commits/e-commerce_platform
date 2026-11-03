import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiGet, refreshAccessToken, setAccessToken } from './client.ts'

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

describe('session (jeton en mémoire + refresh par cookie)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    setAccessToken(null)
  })

  it('n’envoie qu’UNE demande de refresh quand plusieurs arrivent en même temps', async () => {
    const fetchMock = vi.fn().mockResolvedValue(json({ accessToken: 'nouveau', tokenType: 'Bearer', expiresIn: 900 }))
    vi.stubGlobal('fetch', fetchMock)

    const [first, second] = await Promise.all([refreshAccessToken(), refreshAccessToken()])

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(first).toBe('nouveau')
    expect(second).toBe('nouveau')
  })

  it('renouvelle le jeton expiré puis rejoue la requête une seule fois', async () => {
    setAccessToken('expire')
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(json({ title: 'Non authentifié' }, 401))
      .mockResolvedValueOnce(json({ accessToken: 'frais', tokenType: 'Bearer', expiresIn: 900 }))
      .mockResolvedValueOnce(json({ id: 1, email: 'awa@example.com' }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiGet('/users/me')).resolves.toEqual({ id: 1, email: 'awa@example.com' })

    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls[1][0]).toBe('/api/auth/refresh')
    expect(fetchMock.mock.calls[2][1].headers.Authorization).toBe('Bearer frais')
  })
})
