import { ApiError } from '../api/client.ts'

// Erreurs de validation renvoyées par l'API (ProblemDetail.errors), indexées par nom de champ
export function fieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError) || !error.problem?.errors) {
    return {}
  }
  return Object.fromEntries(error.problem.errors.map((e) => [e.field, e.message]))
}
