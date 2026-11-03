import { keepPreviousData, useQuery } from '@tanstack/react-query'
import {
  fetchCategories,
  fetchProduct,
  fetchProducts,
  fetchRating,
  fetchReviews,
  type ProductFilters,
} from '../api/catalog.ts'

// TanStack Query : la clé (queryKey) identifie la donnée en cache. Même clé = pas de nouvel appel réseau
// tant que la donnée est « fraîche » ; chargement, erreur et nouvelle tentative sont gérés pour nous.

export function useProducts(filters: ProductFilters) {
  return useQuery({
    queryKey: ['products', filters],
    queryFn: () => fetchProducts(filters),
    // Garde la page précédente affichée pendant le chargement de la suivante (pas de « clignotement »)
    placeholderData: keepPreviousData,
  })
}

export function useProduct(id: number) {
  return useQuery({
    queryKey: ['product', id],
    queryFn: () => fetchProduct(id),
    enabled: Number.isInteger(id),
  })
}

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: fetchCategories,
    staleTime: 10 * 60_000,
  })
}

export function useReviews(productId: number) {
  return useQuery({
    queryKey: ['reviews', productId],
    queryFn: () => fetchReviews(productId),
    enabled: Number.isInteger(productId),
  })
}

export function useRating(productId: number) {
  return useQuery({
    queryKey: ['rating', productId],
    queryFn: () => fetchRating(productId),
    enabled: Number.isInteger(productId),
  })
}
