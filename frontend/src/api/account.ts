import { apiGet, apiRequest, refreshAccessToken, setAccessToken } from './client.ts'
import type { AccessToken, UserProfile } from './types.ts'

export type Registration = {
  fullName: string
  email: string
  password: string
}

export async function login(email: string, password: string): Promise<UserProfile> {
  const token = await apiRequest<AccessToken>('POST', '/auth/login', { body: { email, password } })
  setAccessToken(token.accessToken)
  return fetchProfile()
}

export function register(registration: Registration): Promise<UserProfile> {
  return apiRequest('POST', '/auth/register', { body: registration })
}

// Le cookie httpOnly est envoyé et supprimé par le navigateur : le frontend n'y touche jamais
export async function logout(): Promise<void> {
  try {
    await apiRequest('POST', '/auth/logout')
  } finally {
    setAccessToken(null)
  }
}

// Au chargement de la page : si le cookie du refresh token est valide, la session reprend sans mot de passe
export async function restoreSession(): Promise<UserProfile | null> {
  return (await refreshAccessToken()) ? fetchProfile() : null
}

export function fetchProfile(): Promise<UserProfile> {
  return apiGet('/users/me')
}
