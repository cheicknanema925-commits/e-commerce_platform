import { createContext, use } from 'react'

// Petit message éphémère en bas de l'écran (« Ajouté au panier »), annoncé aux lecteurs d'écran
export type ShowToast = (message: string) => void

export const ToastContext = createContext<ShowToast>(() => undefined)

export function useToast(): ShowToast {
  return use(ToastContext)
}
