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

export type AccessToken = {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export type UserProfile = {
  id: number
  email: string
  fullName: string
  role: 'CUSTOMER' | 'ADMIN'
}

export type CartItem = {
  productId: number
  productName: string
  unitPrice: number
  quantity: number
  subtotal: number
}

export type Cart = {
  items: CartItem[]
  total: number
}

export type OrderStatus = 'CREATED' | 'PAID' | 'PREPARING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'

export type PaymentMethod = 'ONLINE' | 'CASH_ON_DELIVERY'

export type OrderItem = CartItem

export type Order = {
  id: number
  status: OrderStatus
  paymentMethod: PaymentMethod
  totalAmount: number
  createdAt: string
  items: OrderItem[]
}

export type DeliveryStatus = 'PENDING' | 'PREPARING' | 'SHIPPED' | 'DELIVERED'

export type Delivery = {
  status: DeliveryStatus
  addressLine: string
  city: string
  phone: string
  latitude: number | null
  longitude: number | null
  shippedAt: string | null
  deliveredAt: string | null
}

export type DeliveryAddress = {
  addressLine: string
  city: string
  phone: string
  latitude?: number
  longitude?: number
}

export type PaymentSession = {
  paymentId: number
  status: 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'REFUNDED'
  amount: number
  checkoutUrl: string
}

// Back-office : nextStatuses est calculé par la machine à états du serveur
export type AdminOrder = Order & {
  customerName: string
  customerEmail: string
  nextStatuses: OrderStatus[]
}

export type AdminStats = {
  collectedRevenue: number
  orderCount: number
  customerCount: number
  ordersByStatus: { status: OrderStatus; count: number }[]
  bestSellers: { productId: number; name: string; quantity: number }[]
  lowStock: { productId: number; name: string; stock: number }[]
}

export type ProductInput = {
  categoryId: number
  name: string
  description: string | null
  price: number
  stock: number
}

// Format d'erreur standard de l'API (RFC 9457), produit par GlobalExceptionHandler
export type ProblemDetail = {
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: { field: string; message: string }[]
}
