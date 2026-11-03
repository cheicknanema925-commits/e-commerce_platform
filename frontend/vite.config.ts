import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Le navigateur appelle /api sur ce serveur, qui relaie vers Spring Boot : une seule origine,
    // donc pas besoin d'ouvrir CORS côté API (en production, Caddy jouera ce rôle à l'étape 9)
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
