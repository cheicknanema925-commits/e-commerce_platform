// Les mêmes formes que les DTO (records) renvoyés par Spring Boot

// url relative (/api/images/…) servie par l'API ; la première image est la photo principale
export type ProductImage = {
  id: number
  url: string
}

export type Product = {
  id: number
  name: string
  description: string | null
  price: number
  stock: number
  categoryId: number
  categoryName: string
  images: ProductImage[]
  // Fabricant (facultatif) et référence interne unique (SKU)
  brand: string | null
  sku: string
  // Prix barré et remise calculée par le serveur (null sans promotion)
  compareAtPrice: number | null
  discountPercent: number | null
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

export type OrderItem = {
  productId: number
  productName: string
  unitPrice: number
  quantity: number
  subtotal: number
}

// imageUrl : photo principale du produit (null s'il n'en a pas)
export type CartItem = OrderItem & {
  imageUrl: string | null
}

export type Cart = {
  items: CartItem[]
  total: number
}

export type OrderStatus = 'CREATED' | 'PAID' | 'PREPARING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'

export type PaymentMethod = 'ONLINE' | 'CASH_ON_DELIVERY'

export type Order = {
  id: number
  status: OrderStatus
  paymentMethod: PaymentMethod
  totalAmount: number
  // Frais de service (espèces à la livraison), déjà inclus dans totalAmount
  serviceFee: number
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
  // Livreur : visible par le client une fois le colis parti
  courierName: string | null
  courierPhone: string | null
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
  courierId: number | null
  courierName: string | null
}

export type Courier = {
  id: number
  name: string
  phone: string
  active: boolean
  ongoingDeliveries: number
}

export type CourierInput = {
  name: string
  phone: string
  active: boolean
}

// Paramètres de la boutique (publics : la page de commande en a besoin). codZones vide = partout
export type StoreSettings = {
  storeName: string
  supportEmail: string
  onlinePaymentEnabled: boolean
  codEnabled: boolean
  codMaxAmount: number | null
  codFee: number
  codZones: string[]
}

export type Subscriber = {
  id: number
  email: string
  createdAt: string
}

// Montants « encaissés » = paiements réussis (en ligne confirmés + espèces reçues à la livraison)
export type PeriodSummary = {
  collected: number
  orders: number
}

export type AdminStats = {
  collectedRevenue: number
  orderCount: number
  customerCount: number
  today: PeriodSummary
  thisMonth: PeriodSummary
  awaitingConfirmation: number
  averageOrderValue: number
  // cashToCollect : espèces que les livreurs doivent encore remettre (commandes ni livrées ni annulées)
  payments: { collectedOnline: number; collectedCash: number; cashToCollect: number; cashToCollectOrders: number }
  // repeat : clients ayant au moins 2 commandes non annulées
  customers: { newThisMonth: number; repeat: number }
  ordersByStatus: { status: OrderStatus; count: number }[]
  salesByCategory: { category: string; quantity: number; revenue: number }[]
  bestSellers: { productId: number; name: string; quantity: number; imageUrl: string | null }[]
  lowStock: { productId: number; name: string; stock: number; imageUrl: string | null }[]
  catalog: { productCount: number; outOfStock: number; stockValue: number }
}

export type SalesPeriod = 'DAY' | 'MONTH'

// Un point par jour (ou par mois), y compris les périodes sans vente ; start au format AAAA-MM-JJ
export type SalesReport = {
  period: SalesPeriod
  points: { start: string; collected: number; orders: number }[]
}

export type AdminCustomer = {
  id: number
  fullName: string
  email: string
  createdAt: string
  orderCount: number
  totalSpent: number
  lastOrderAt: string | null
}

export type ProductInput = {
  categoryId: number
  name: string
  description: string | null
  price: number
  stock: number
  brand: string | null
  // null = référence générée par le serveur
  sku: string | null
  compareAtPrice: number | null
}

// Format d'erreur standard de l'API (RFC 9457), produit par GlobalExceptionHandler
// Service client : demandes (tickets) et pré-commandes
export type TicketCategory = 'ORDER' | 'DELIVERY' | 'PAYMENT' | 'PRODUCT' | 'OTHER'

export type TicketStatus = 'OPEN' | 'ANSWERED' | 'CLOSED'

export type SupportTicket = {
  id: number
  orderId: number | null
  category: TicketCategory
  subject: string
  message: string
  status: TicketStatus
  reply: string | null
  createdAt: string
  repliedAt: string | null
}

export type TicketInput = { category: TicketCategory; subject: string; message: string; orderId: number | null }

export type AdminTicket = { ticket: SupportTicket; customerName: string; customerEmail: string }

export type PreOrderStatus = 'WAITING' | 'NOTIFIED' | 'CANCELLED'

export type PreOrder = {
  id: number
  productId: number
  productName: string
  quantity: number
  status: PreOrderStatus
  createdAt: string
  notifiedAt: string | null
}

export type PreOrderDemand = { productId: number; productName: string; requests: number; quantity: number }

// Journal des changements de prix (assistant admin)
export type PriceChange = {
  id: number
  batchId: string
  productId: number
  productName: string
  authorName: string | null
  kind: 'PROMOTION' | 'PRICE'
  oldPrice: number
  newPrice: number
  oldCompareAtPrice: number | null
  newCompareAtPrice: number | null
  endsAt: string | null
  createdAt: string
  revertedAt: string | null
  revertReason: 'UNDO' | 'EXPIRED' | 'OVERRIDDEN' | null
}

export type ProblemDetail = {
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: { field: string; message: string }[]
}
