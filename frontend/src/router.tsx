import { createBrowserRouter } from 'react-router'
import { Layout } from './components/Layout.tsx'
import { RequireAuth } from './components/RequireAuth.tsx'
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
// Les pages sous RequireAuth demandent une connexion (l'API, elle, revérifie le jeton à chaque appel).
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
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
