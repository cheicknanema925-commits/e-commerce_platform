# Frontend de la boutique

React 19 + TypeScript + Vite + Tailwind CSS, React Router et TanStack Query.

## Lancer en local

Prérequis : Node.js 24 et l'API Spring Boot démarrée sur le port 8080.

```bash
npm install
npm run dev        # http://localhost:5173 (les appels /api sont relayés vers http://localhost:8080)
```

## Commandes

```bash
npm run lint       # analyse du code (oxlint)
npm test           # tests (Vitest)
npm run build      # vérification TypeScript + build de production dans dist/
```

## Organisation

```
src/
├── api/          appels à l'API et types des réponses (comme des repositories côté Spring)
├── hooks/        accès aux données avec cache (TanStack Query)
├── components/   briques réutilisables (carte produit, pagination…)
├── pages/        une page par route (catalogue, fiche produit…)
├── lib/          utilitaires (format des prix en francs CFA…)
└── router.tsx    les routes de l'application
```
