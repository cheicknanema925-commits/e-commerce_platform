import type { AccessToken, ProblemDetail } from './types.ts'

// Erreur renvoyée par l'API, avec son ProblemDetail quand il existe
export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(status: number, problem: ProblemDetail | null) {
    super(problem?.detail ?? `Erreur ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

type QueryParams = Record<string, string | number | undefined>

type RequestOptions = {
  body?: unknown
  params?: QueryParams
}

// Jeton d'accès gardé UNIQUEMENT en mémoire (jamais dans localStorage, lisible par n'importe quel script).
// Après un rechargement de page, on le redemande grâce au cookie httpOnly du refresh token.
let accessToken: string | null = null
let refreshInFlight: Promise<string | null> | null = null
let sessionExpiredListener: (() => void) | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function onSessionExpired(listener: () => void) {
  sessionExpiredListener = listener
}

// Une seule demande à la fois : deux renouvellements simultanés avec le même cookie seraient vus par l'API
// comme la réutilisation d'un refresh token volé, et toutes les sessions seraient coupées
export function refreshAccessToken(): Promise<string | null> {
  refreshInFlight ??= fetch('/api/auth/refresh', { method: 'POST', credentials: 'same-origin' })
    .then(async (response) => {
      const token = response.ok ? ((await response.json()) as AccessToken).accessToken : null
      setAccessToken(token)
      return token
    })
    .catch(() => null)
    .finally(() => {
      refreshInFlight = null
    })
  return refreshInFlight
}

// Tous les appels passent par ici : adresse /api (relayée vers Spring Boot), JSON, jeton, erreurs uniformes
export async function apiRequest<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const url = buildUrl(path, options.params)
  const send = () => {
    const headers: Record<string, string> = { Accept: 'application/json' }
    // FormData (envoi de fichier) : le navigateur écrit lui-même le Content-Type multipart et sa « boundary »
    const isForm = options.body instanceof FormData
    if (options.body !== undefined && !isForm) {
      headers['Content-Type'] = 'application/json'
    }
    if (accessToken) {
      headers.Authorization = `Bearer ${accessToken}`
    }
    return fetch(url, {
      method,
      headers,
      credentials: 'same-origin',
      body: isForm ? (options.body as FormData) : options.body !== undefined ? JSON.stringify(options.body) : undefined,
    })
  }

  let response = await send()
  // Jeton d'accès expiré (15 min) : on en redemande un avec le cookie, puis on rejoue la requête une seule fois
  if (response.status === 401 && accessToken !== null) {
    if (await refreshAccessToken()) {
      response = await send()
    } else {
      sessionExpiredListener?.()
    }
  }
  return readResponse<T>(response)
}

// Fichier protégé (export CSV) : on le récupère avec le jeton, puis on déclenche l'enregistrement dans le navigateur
export async function apiDownload(path: string, fallbackName: string): Promise<void> {
  const send = () =>
    fetch(buildUrl(path), { headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {}, credentials: 'same-origin' })
  let response = await send()
  if (response.status === 401 && accessToken !== null && (await refreshAccessToken())) {
    response = await send()
  }
  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
  }
  const name = /filename="([^"]+)"/.exec(response.headers.get('Content-Disposition') ?? '')?.[1] ?? fallbackName
  const url = URL.createObjectURL(await response.blob())
  const link = Object.assign(document.createElement('a'), { href: url, download: name })
  link.click()
  URL.revokeObjectURL(url)
}

export function apiGet<T>(path: string, params: QueryParams = {}): Promise<T> {
  return apiRequest<T>('GET', path, { params })
}

export async function readResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
  }
  if (response.status === 204 || response.headers.get('Content-Length') === '0') {
    return undefined as T
  }
  return (await response.json()) as T
}

function buildUrl(path: string, params: QueryParams = {}): string {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      query.set(key, String(value))
    }
  }
  return query.size > 0 ? `/api${path}?${query}` : `/api${path}`
}

async function readProblem(response: Response): Promise<ProblemDetail | null> {
  if (!(response.headers.get('Content-Type') ?? '').includes('json')) {
    return null
  }
  try {
    return (await response.json()) as ProblemDetail
  } catch {
    return null
  }
}
