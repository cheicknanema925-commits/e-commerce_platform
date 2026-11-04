import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Le navigateur appelle /api sur ce serveur, qui relaie vers Spring Boot : une seule origine,
    // donc pas besoin d'ouvrir CORS côté API (en production, Caddy jouera ce rôle à l'étape 9)
    // API_URL (facultatif) : viser une autre API que celle du port 8080 (tests, Docker)
    proxy: {
      '/api': process.env.API_URL ?? 'http://localhost:8080',
    },
  },
})
