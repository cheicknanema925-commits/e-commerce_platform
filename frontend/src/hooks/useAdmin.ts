import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  changeOrderStatus,
  createProduct,
  deleteProduct,
  fetchAdminDelivery,
  fetchAdminOrder,
  fetchAdminOrders,
  fetchStats,
  updateProduct,
} from '../api/admin.ts'
import type { OrderStatus } from '../api/types.ts'

export function useStats() {
  return useQuery({ queryKey: ['admin', 'stats'], queryFn: fetchStats })
}

export function useAdminOrders(status: OrderStatus | undefined, page: number) {
  return useQuery({
    queryKey: ['admin', 'orders', status, page],
    queryFn: () => fetchAdminOrders(status, page),
    placeholderData: keepPreviousData,
  })
}

export function useAdminOrder(id: number) {
  return useQuery({ queryKey: ['admin', 'orders', id], queryFn: () => fetchAdminOrder(id), enabled: Number.isInteger(id) })
}

export function useAdminDelivery(orderId: number) {
  return useQuery({
    queryKey: ['admin', 'orders', orderId, 'delivery'],
    queryFn: () => fetchAdminDelivery(orderId),
    enabled: Number.isInteger(orderId),
    retry: false,
  })
}

// Après une modification, on « invalide » les données concernées : TanStack Query les recharge au besoin
export function useChangeOrderStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: changeOrderStatus,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin'] }),
  })
}

function useProductMutation<V, R>(mutationFn: (variables: V) => Promise<R>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
      queryClient.invalidateQueries({ queryKey: ['product'] })
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] })
    },
  })
}

export function useCreateProduct() {
  return useProductMutation(createProduct)
}

export function useUpdateProduct() {
  return useProductMutation(updateProduct)
}

export function useDeleteProduct() {
  return useProductMutation(deleteProduct)
}
