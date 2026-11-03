import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { addToCart, changeQuantity, fetchCart, removeFromCart } from '../api/cart.ts'
import type { Cart } from '../api/types.ts'
import { useAuth } from '../auth/useAuth.ts'

// Le panier n'existe que pour un client connecté (il est lié à son compte côté API)
export function useCart() {
  const { status } = useAuth()
  return useQuery({ queryKey: ['cart'], queryFn: fetchCart, enabled: status === 'authenticated' })
}

// Chaque modification renvoie le panier à jour : on remplace directement le cache, sans nouvel appel
function useCartMutation<V>(mutationFn: (variables: V) => Promise<Cart>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: (cart) => queryClient.setQueryData(['cart'], cart),
  })
}

export function useAddToCart() {
  return useCartMutation(addToCart)
}

export function useChangeQuantity() {
  return useCartMutation(changeQuantity)
}

export function useRemoveFromCart() {
  return useCartMutation(removeFromCart)
}
