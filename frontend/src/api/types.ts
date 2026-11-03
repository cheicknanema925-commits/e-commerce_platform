// Les mêmes formes que les DTO (records) renvoyés par Spring Boot

export type Product = {
  id: number
  name: string
  description: string | null
  price: number
  stock: number
  categoryId: number
  categoryName: string
}

export type Category = {
  id: number
  name: string
  slug: string
}

export type Page<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type Review = {
  id: number
  rating: number
  comment: string | null
  author: string
  createdAt: string
}

export type Rating = {
  average: number
  count: number
}

// Format d'erreur standard de l'API (RFC 9457), produit par GlobalExceptionHandler
export type ProblemDetail = {
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: { field: string; message: string }[]
}
