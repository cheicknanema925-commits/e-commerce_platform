// Visuel de chaque rayon (emoji + couleur de fond), repris de la maquette. Une catégorie ajoutée plus tard
// en base sans visuel prévu reçoit le visuel par défaut.
export type CategoryVisual = { emoji: string; tint: string }

const VISUALS: Record<string, CategoryVisual> = {
  mode: { emoji: '👗', tint: 'bg-tint-bissap' },
  beaute: { emoji: '🧴', tint: 'bg-tint-violet' },
  electronique: { emoji: '💻', tint: 'bg-tint-sky' },
  maison: { emoji: '🛋️', tint: 'bg-tint-kola' },
  alimentation: { emoji: '🥭', tint: 'bg-tint-safran' },
}

const DEFAULT_VISUAL: CategoryVisual = { emoji: '📦', tint: 'bg-tint-indigo' }

export function categoryVisual(slug: string | undefined): CategoryVisual {
  return (slug && VISUALS[slug]) || DEFAULT_VISUAL
}
