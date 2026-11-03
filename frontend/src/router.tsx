import { createBrowserRouter } from 'react-router'
import { Layout } from './components/Layout.tsx'
import { RequireAdmin } from './components/RequireAdmin.tsx'
import { RequireAuth } from './components/RequireAuth.tsx'
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage.tsx'
import { AdminLayout } from './pages/admin/AdminLayout.tsx'
import { AdminOrderPage } from './pages/admin/AdminOrderPage.tsx'
import { AdminOrdersPage } from './pages/admin/AdminOrdersPage.tsx'
import { AdminProductFormPage } from './pages/admin/AdminProductFormPage.tsx'
import { AdminProductsPage } from './pages/admin/AdminProductsPage.tsx'
import { CartPage } from './pages/CartPage.tsx'
import { CatalogPage } from './pages/CatalogPage.tsx'
import { CheckoutPage } from './pages/CheckoutPage.tsx'
import { LoginPage } from './pages/LoginPage.tsx'
import { NotFoundPage } from './pages/NotFoundPage.tsx'
import { OrderPage } from './pages/OrderPage.tsx'
import { OrdersPage } from './pages/OrdersPage.tsx'
import { ProductPage } from './pages/ProductPage.tsx'
import { RegisterPage } from './pages/RegisterPage.tsx'
import { SimulatedPaymentPage } from './pages/SimulatedPaymentPage.tsx'

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
      {
        element: <RequireAuth />,
        children: [
          { path: 'panier', element: <CartPage /> },
          { path: 'commande', element: <CheckoutPage /> },
          { path: 'commandes', element: <OrdersPage /> },
          { path: 'commandes/:id', element: <OrderPage /> },
          { path: 'paiement-simule/:reference', element: <SimulatedPaymentPage /> },
          {
            path: 'admin',
            element: <RequireAdmin />,
            children: [
              {
                element: <AdminLayout />,
                children: [
                  { index: true, element: <AdminDashboardPage /> },
                  { path: 'produits', element: <AdminProductsPage /> },
                  { path: 'produits/nouveau', element: <AdminProductFormPage /> },
                  { path: 'produits/:id', element: <AdminProductFormPage /> },
                  { path: 'commandes', element: <AdminOrdersPage /> },
                  { path: 'commandes/:id', element: <AdminOrderPage /> },
                ],
              },
            ],
          },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
