import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchFavoriteIds, fetchFavorites, fetchStoreSettings, setFavorite, subscribeToNewsletter, unsubscribeFromNewsletter } from '../api/shop.ts'
import { useAuth } from '../auth/useAuth.ts'

export function useFavorites() {
  const { status } = useAuth()
  return useQuery({ queryKey: ['favorites', 'list'], queryFn: fetchFavorites, enabled: status === 'authenticated' })
}

// Ids des favoris : un seul appel pour colorer tous les cœurs d'une page
export function useFavoriteIds() {
  const { status } = useAuth()
  return useQuery({ queryKey: ['favorites', 'ids'], queryFn: fetchFavoriteIds, enabled: status === 'authenticated' })
}

// Mise à jour « optimiste » : le cœur change tout de suite ; en cas d'erreur, on revient à l'état précédent
export function useToggleFavorite() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: setFavorite,
    onMutate: async ({ productId, favorite }) => {
      await queryClient.cancelQueries({ queryKey: ['favorites', 'ids'] })
      const previous = queryClient.getQueryData<number[]>(['favorites', 'ids'])
      queryClient.setQueryData<number[]>(['favorites', 'ids'], (ids = []) =>
        favorite ? [...new Set([...ids, productId])] : ids.filter((id) => id !== productId),
      )
      return { previous }
    },
    onError: (_error, _input, context) => queryClient.setQueryData(['favorites', 'ids'], context?.previous),
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['favorites'] }),
  })
}

export function useStoreSettings() {
  return useQuery({ queryKey: ['store-settings'], queryFn: fetchStoreSettings, staleTime: 5 * 60_000 })
}

export function useSubscribe() {
  return useMutation({ mutationFn: subscribeToNewsletter })
}

export function useUnsubscribe() {
  return useMutation({ mutationFn: unsubscribeFromNewsletter })
}
