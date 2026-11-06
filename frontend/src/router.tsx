import { createBrowserRouter } from 'react-router'
import { Layout } from './components/Layout.tsx'
import { RequireAdmin } from './components/RequireAdmin.tsx'
import { RequireAuth } from './components/RequireAuth.tsx'
import { AdminAnalyticsPage } from './pages/admin/AdminAnalyticsPage.tsx'
import { AdminAssistantPage } from './pages/admin/AdminAssistantPage.tsx'
import { AdminCouriersPage } from './pages/admin/AdminCouriersPage.tsx'
import { AdminCustomersPage } from './pages/admin/AdminCustomersPage.tsx'
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage.tsx'
import { AdminLayout } from './pages/admin/AdminLayout.tsx'
import { AdminNewsletterPage } from './pages/admin/AdminNewsletterPage.tsx'
import { AdminOrderPage } from './pages/admin/AdminOrderPage.tsx'
import { AdminOrdersPage } from './pages/admin/AdminOrdersPage.tsx'
import { AdminPaymentsPage } from './pages/admin/AdminPaymentsPage.tsx'
import { AdminPriceChangesPage } from './pages/admin/AdminPriceChangesPage.tsx'
import { AdminProductFormPage } from './pages/admin/AdminProductFormPage.tsx'
import { AdminProductsPage } from './pages/admin/AdminProductsPage.tsx'
import { AdminSettingsPage } from './pages/admin/AdminSettingsPage.tsx'
import { AdminSupportPage } from './pages/admin/AdminSupportPage.tsx'
import { CartPage } from './pages/CartPage.tsx'
import { CatalogPage } from './pages/CatalogPage.tsx'
import { CheckoutPage } from './pages/CheckoutPage.tsx'
import { FavoritesPage } from './pages/FavoritesPage.tsx'
import { LoginPage } from './pages/LoginPage.tsx'
import { NotFoundPage } from './pages/NotFoundPage.tsx'
import { OrderPage } from './pages/OrderPage.tsx'
import { OrdersPage } from './pages/OrdersPage.tsx'
import { ProductPage } from './pages/ProductPage.tsx'
import { RegisterPage } from './pages/RegisterPage.tsx'
import { SimulatedPaymentPage } from './pages/SimulatedPaymentPage.tsx'
import { SupportPage } from './pages/SupportPage.tsx'
import { UnsubscribePage } from './pages/UnsubscribePage.tsx'

// Comme les @GetMapping d'un contrôleur : chaque adresse affiche une page, à l'intérieur du Layout commun.
// Les pages sous RequireAuth demandent une connexion, celles sous RequireAdmin le rôle ADMIN
// (l'API, elle, revérifie le jeton et le rôle à chaque appel).
export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <CatalogPage /> },
      { path: 'produits/:id', element: <ProductPage /> },
      { path: 'connexion', element: <LoginPage /> },
      { path: 'inscription', element: <RegisterPage /> },
      { path: 'desinscription', element: <UnsubscribePage /> },
      {
        element: <RequireAuth />,
        children: [
          { path: 'panier', element: <CartPage /> },
          { path: 'favoris', element: <FavoritesPage /> },
          { path: 'commande', element: <CheckoutPage /> },
          { path: 'commandes', element: <OrdersPage /> },
          { path: 'commandes/:id', element: <OrderPage /> },
          { path: 'demandes', element: <SupportPage /> },
          { path: 'paiement-simule/:reference', element: <SimulatedPaymentPage /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  // Back-office : hors du Layout de la boutique, avec son propre cadre (AdminLayout)
  {
    path: '/admin',
    element: <RequireAuth />,
    children: [
      {
        element: <RequireAdmin />,
        children: [
          {
            element: <AdminLayout />,
            children: [
              { index: true, element: <AdminDashboardPage /> },
              { path: 'commandes', element: <AdminOrdersPage /> },
              { path: 'commandes/:id', element: <AdminOrderPage /> },
              { path: 'produits', element: <AdminProductsPage /> },
              { path: 'produits/nouveau', element: <AdminProductFormPage /> },
              { path: 'produits/:id', element: <AdminProductFormPage /> },
              { path: 'paiements', element: <AdminPaymentsPage /> },
              { path: 'analyse', element: <AdminAnalyticsPage /> },
              { path: 'assistant', element: <AdminAssistantPage /> },
              { path: 'demandes', element: <AdminSupportPage /> },
              { path: 'prix', element: <AdminPriceChangesPage /> },
              { path: 'clients', element: <AdminCustomersPage /> },
              { path: 'infolettre', element: <AdminNewsletterPage /> },
              { path: 'livreurs', element: <AdminCouriersPage /> },
              { path: 'parametres', element: <AdminSettingsPage /> },
              { path: '*', element: <NotFoundPage /> },
            ],
          },
        ],
      },
    ],
  },
])
