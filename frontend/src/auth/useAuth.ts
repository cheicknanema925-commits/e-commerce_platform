import { createContext, use } from 'react'
import type { Registration } from '../api/account.ts'
import type { UserProfile } from '../api/types.ts'

// Comme le SecurityContext de Spring : « qui est connecté », accessible depuis n'importe quel composant
export type AuthContextValue = {
  status: 'loading' | 'authenticated' | 'anonymous'
  user: UserProfile | null
  login: (email: string, password: string) => Promise<void>
  register: (registration: Registration) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const context = use(AuthContext)
  if (!context) {
    throw new Error('useAuth doit être utilisé à l’intérieur de <AuthProvider>')
  }
  return context
}
