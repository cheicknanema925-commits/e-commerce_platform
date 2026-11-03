import { apiGet, apiRequest } from './client.ts'
import type { AdminOrder, AdminStats, Delivery, OrderStatus, Page, Product, ProductInput } from './types.ts'

// Routes /api/admin/** : l'API vérifie le rôle ADMIN à chaque appel (le masquage des menus n'est qu'un confort)

export function fetchStats(): Promise<AdminStats> {
  return apiGet('/admin/stats')
}

export function fetchAdminOrders(status: OrderStatus | undefined, page: number): Promise<Page<AdminOrder>> {
  return apiGet('/admin/orders', { status, page, size: 20 })
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

export function createProduct(product: ProductInput): Promise<Product> {
  return apiRequest('POST', '/products', { body: product })
}

export function updateProduct(input: { id: number; product: ProductInput }): Promise<Product> {
  return apiRequest('PUT', `/products/${input.id}`, { body: input.product })
}

export function deleteProduct(id: number): Promise<void> {
  return apiRequest('DELETE', `/products/${id}`)
}
