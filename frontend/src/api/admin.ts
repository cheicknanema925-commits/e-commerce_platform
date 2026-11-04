import { apiGet, apiRequest } from './client.ts'
import type {
  AdminCustomer,
  AdminOrder,
  AdminStats,
  Delivery,
  OrderStatus,
  Page,
  PaymentMethod,
  Product,
  ProductInput,
  SalesPeriod,
  SalesReport,
} from './types.ts'

// Routes /api/admin/** : l'API vérifie le rôle ADMIN à chaque appel (le masquage des menus n'est qu'un confort)

export function fetchStats(): Promise<AdminStats> {
  return apiGet('/admin/stats')
}

export function fetchSales(period: SalesPeriod): Promise<SalesReport> {
  return apiGet('/admin/stats/sales', { period })
}

export type AdminOrderFilters = {
  statuses?: OrderStatus[]
  paymentMethod?: PaymentMethod
  q?: string
  page: number
  size?: number
}

// Plusieurs statuts séparés par des virgules (?status=CREATED,PREPARING) : Spring en fait une liste
export function fetchAdminOrders({ statuses, size = 20, ...filters }: AdminOrderFilters): Promise<Page<AdminOrder>> {
  return apiGet('/admin/orders', { ...filters, status: statuses?.join(','), size })
}

export function fetchAdminOrder(id: number): Promise<AdminOrder> {
  return apiGet(`/admin/orders/${id}`)
}

export function fetchAdminDelivery(orderId: number): Promise<Delivery> {
  return apiGet(`/admin/orders/${orderId}/delivery`)
}

export function changeOrderStatus(input: { id: number; status: OrderStatus }): Promise<AdminOrder> {
  return apiRequest('PATCH', `/admin/orders/${input.id}/status`, { body: { status: input.status } })
}

export function fetchCustomers(q: string | undefined, page: number): Promise<Page<AdminCustomer>> {
  return apiGet('/admin/customers', { q, page, size: 20 })
}

export function createProduct(product: ProductInput): Promise<Product> {
  return apiRequest('POST', '/products', { body: product })
}

export function updateProduct(input: { id: number; product: ProductInput }): Promise<Product> {
  return apiRequest('PUT', `/products/${input.id}`, { body: input.product })
}

export function deleteProduct(id: number): Promise<void> {
  return apiRequest('DELETE', `/products/${id}`)
}

// Envoi multipart : le serveur vérifie, réduit et réenregistre la photo (JPEG ou PNG, 5 Mo maximum)
export function uploadProductImage(input: { productId: number; file: File }): Promise<Product> {
  const form = new FormData()
  form.append('file', input.file)
  return apiRequest('POST', `/products/${input.productId}/images`, { body: form })
}

export function makeMainProductImage(input: { productId: number; imageId: number }): Promise<Product> {
  return apiRequest('PUT', `/products/${input.productId}/images/${input.imageId}/main`)
}

export function deleteProductImage(input: { productId: number; imageId: number }): Promise<void> {
  return apiRequest('DELETE', `/products/${input.productId}/images/${input.imageId}`)
}
