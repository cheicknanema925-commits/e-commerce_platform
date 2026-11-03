import { useQueryClient } from '@tanstack/react-query'
import { type ReactNode, useEffect, useMemo, useState } from 'react'
import * as account from '../api/account.ts'
import { onSessionExpired } from '../api/client.ts'
import type { UserProfile } from '../api/types.ts'
import { AuthContext, type AuthContextValue } from './useAuth.ts'

type Session = { status: AuthContextValue['status']; user: UserProfile | null }

const ANONYMOUS: Session = { status: 'anonymous', user: null }

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, setSession] = useState<Session>({ status: 'loading', user: null })

  useEffect(() => {
    let cancelled = false
    // Au chargement : le cookie httpOnly permet de reprendre la session sans redemander le mot de passe
    account.restoreSession().then((user) => {
      if (!cancelled) {
        setSession(user ? { status: 'authenticated', user } : ANONYMOUS)
      }
    })
    // Refresh token expiré ou révoqué pendant la navigation : on repasse en visiteur et on vide les données privées
    onSessionExpired(() => {
      queryClient.removeQueries({ queryKey: ['cart'] })
      queryClient.removeQueries({ queryKey: ['orders'] })
      setSession(ANONYMOUS)
    })
    return () => {
      cancelled = true
    }
  }, [queryClient])

  const value = useMemo<AuthContextValue>(
    () => ({
      ...session,
      async login(email, password) {
        const user = await account.login(email, password)
        setSession({ status: 'authenticated', user })
      },
      async register(registration) {
        await account.register(registration)
        const user = await account.login(registration.email, registration.password)
        setSession({ status: 'authenticated', user })
      },
      async logout() {
        await account.logout()
        // Les données d'un client ne doivent jamais rester en cache pour le suivant (ordinateur partagé)
        queryClient.clear()
        setSession(ANONYMOUS)
      },
    }),
    [session, queryClient],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
