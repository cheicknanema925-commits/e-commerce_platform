import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cancelOrder, checkout, fetchDelivery, fetchMyOrders, fetchOrder, startPayment } from '../api/orders.ts'

export function useMyOrders() {
  return useQuery({ queryKey: ['orders'], queryFn: fetchMyOrders })
}

// waitForPayment : au retour de la page de paiement, on interroge l'API toutes les 2 s tant que la commande
// n'est pas confirmée. On n'affiche « payée » que lorsque le SERVEUR l'a confirmé (webhook vérifié).
export function useOrder(id: number, waitForPayment = false) {
  return useQuery({
    queryKey: ['orders', id],
    queryFn: () => fetchOrder(id),
    enabled: Number.isInteger(id),
    refetchInterval: (query) => (waitForPayment && query.state.data?.status === 'CREATED' ? 2000 : false),
  })
}

export function useDelivery(orderId: number) {
  return useQuery({
    queryKey: ['orders', orderId, 'delivery'],
    queryFn: () => fetchDelivery(orderId),
    enabled: Number.isInteger(orderId),
    retry: false,
  })
}

export function useCheckout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: checkout,
    onSuccess: () => {
      // La commande vide le panier côté API : on rafraîchit le panier et la liste des commandes
      queryClient.invalidateQueries({ queryKey: ['cart'] })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })
}

export function useCancelOrder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: cancelOrder,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['orders'] }),
  })
}

export function useStartPayment() {
  return useMutation({ mutationFn: startPayment })
}
