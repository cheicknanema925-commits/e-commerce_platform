import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cancelPreOrder,
  closeTicket,
  fetchAdminTickets,
  fetchMyPreOrders,
  fetchMyTickets,
  fetchPreOrderDemand,
  fetchPriceChanges,
  openTicket,
  preOrder,
  replyToTicket,
} from '../api/support.ts'
import type { TicketStatus } from '../api/types.ts'
import { useAuth } from '../auth/useAuth.ts'

export function useMyTickets() {
  return useQuery({ queryKey: ['support', 'mine'], queryFn: fetchMyTickets })
}

export function useOpenTicket() {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: openTicket, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['support'] }) })
}

export function useMyPreOrders() {
  const { status } = useAuth()
  return useQuery({ queryKey: ['preorders'], queryFn: fetchMyPreOrders, enabled: status === 'authenticated' })
}

export function usePreOrder() {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: preOrder, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['preorders'] }) })
}

export function useCancelPreOrder() {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: cancelPreOrder, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['preorders'] }) })
}

export function useAdminTickets(status: TicketStatus | undefined, page: number) {
  return useQuery({
    queryKey: ['admin', 'tickets', status, page],
    queryFn: () => fetchAdminTickets(status, page),
    placeholderData: keepPreviousData,
  })
}

export function useReplyToTicket() {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: replyToTicket, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'tickets'] }) })
}

export function useCloseTicket() {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: closeTicket, onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'tickets'] }) })
}

export function usePreOrderDemand() {
  return useQuery({ queryKey: ['admin', 'preorders'], queryFn: fetchPreOrderDemand })
}

export function usePriceChanges(page: number) {
  return useQuery({ queryKey: ['price-changes', page], queryFn: () => fetchPriceChanges(page), placeholderData: keepPreviousData })
}
