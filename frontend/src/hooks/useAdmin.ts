import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  type AdminOrderFilters,
  changeOrderStatus,
  createProduct,
  deleteProduct,
  deleteProductImage,
  fetchAdminDelivery,
  fetchAdminOrder,
  fetchAdminOrders,
  fetchCustomers,
  fetchSales,
  fetchStats,
  makeMainProductImage,
  updateProduct,
  uploadProductImage,
} from '../api/admin.ts'
import type { SalesPeriod } from '../api/types.ts'

// Le tableau de bord se rafraîchit seul toutes les minutes (nouvelles commandes, paiements reçus)
export function useStats() {
  return useQuery({ queryKey: ['admin', 'stats'], queryFn: fetchStats, refetchInterval: 60_000 })
}

export function useSales(period: SalesPeriod) {
  return useQuery({
    queryKey: ['admin', 'sales', period],
    queryFn: () => fetchSales(period),
    // Garde le graphique précédent affiché pendant le chargement de l'autre période (pas de « trou »)
    placeholderData: keepPreviousData,
  })
}

export function useAdminOrders(filters: AdminOrderFilters) {
  return useQuery({
    queryKey: ['admin', 'orders', filters],
    queryFn: () => fetchAdminOrders(filters),
    placeholderData: keepPreviousData,
  })
}

export function useAdminOrder(id: number) {
  return useQuery({ queryKey: ['admin', 'order', id], queryFn: () => fetchAdminOrder(id), enabled: Number.isInteger(id) })
}

export function useAdminDelivery(orderId: number) {
  return useQuery({
    queryKey: ['admin', 'order', orderId, 'delivery'],
    queryFn: () => fetchAdminDelivery(orderId),
    enabled: Number.isInteger(orderId),
    retry: false,
  })
}

export function useCustomers(q: string | undefined, page: number) {
  return useQuery({
    queryKey: ['admin', 'customers', q, page],
    queryFn: () => fetchCustomers(q, page),
    placeholderData: keepPreviousData,
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

export function useUploadProductImage() {
  return useProductMutation(uploadProductImage)
}

export function useMakeMainProductImage() {
  return useProductMutation(makeMainProductImage)
}

export function useDeleteProductImage() {
  return useProductMutation(deleteProductImage)
}
