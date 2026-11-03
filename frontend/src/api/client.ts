import type { ProblemDetail } from './types.ts'

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

// Tous les appels passent par ici : adresse /api (relayée vers Spring Boot), JSON, erreurs uniformes
export async function apiGet<T>(path: string, params: QueryParams = {}): Promise<T> {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '') {
      query.set(key, String(value))
    }
  }
  const url = query.size > 0 ? `/api${path}?${query}` : `/api${path}`
  const response = await fetch(url, { headers: { Accept: 'application/json' } })
  return readResponse<T>(response)
}

export async function readResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
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
