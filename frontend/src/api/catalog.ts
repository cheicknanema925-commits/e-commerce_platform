import { apiGet } from './client.ts'
import type { Category, Page, Product, Rating, Review } from './types.ts'

export const PAGE_SIZE = 12

// Tri au format Spring Data : « propriété,sens »
export type ProductSort = 'id,desc' | 'price,asc' | 'price,desc' | 'name,asc'

export type ProductFilters = {
  q?: string
  category?: string
  sort?: ProductSort
  page?: number
}

export function fetchProducts(filters: ProductFilters): Promise<Page<Product>> {
  return apiGet('/products', {
    q: filters.q,
    category: filters.category,
    sort: filters.sort ?? 'id,desc',
    page: filters.page ?? 0,
    size: PAGE_SIZE,
  })
}

export function fetchProduct(id: number): Promise<Product> {
  return apiGet(`/products/${id}`)
}

export function fetchCategories(): Promise<Category[]> {
  return apiGet('/categories')
}

export function fetchReviews(productId: number): Promise<Page<Review>> {
  return apiGet(`/products/${productId}/reviews`, { size: 20 })
}

export function fetchRating(productId: number): Promise<Rating> {
  return apiGet(`/products/${productId}/rating`)
}
