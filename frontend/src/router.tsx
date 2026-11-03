import { createBrowserRouter } from 'react-router'
import { Layout } from './components/Layout.tsx'
import { CatalogPage } from './pages/CatalogPage.tsx'
import { NotFoundPage } from './pages/NotFoundPage.tsx'
import { ProductPage } from './pages/ProductPage.tsx'

// Comme les @GetMapping d'un contrôleur : chaque adresse affiche une page, à l'intérieur du Layout commun
export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <CatalogPage /> },
      { path: 'produits/:id', element: <ProductPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
