// Icônes au trait de la maquette (currentColor : elles prennent la couleur du texte). Toujours décoratives :
// le texte à côté (ou un aria-label sur le bouton) porte le sens.
const PATHS = {
  user: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21c1.5-4 4.5-6 8-6s6.5 2 8 6',
  cart: 'M3 4h2l2.2 11h11.3L21 8H7M9 19.5h.01M17 19.5h.01',
  bag: 'M6 7h12l-1 13H7L6 7zM9 7a3 3 0 0 1 6 0',
  truck: 'M2 7h11v9H2zM13 10h4l3 3v3h-7M6 19.3a1.8 1.8 0 1 0 0-3.6 1.8 1.8 0 0 0 0 3.6zM17 19.3a1.8 1.8 0 1 0 0-3.6 1.8 1.8 0 0 0 0 3.6z',
  lock: 'M4 10h16v11H4zM8 10V7a4 4 0 0 1 8 0v3',
  return: 'M4 12a8 8 0 1 0 2.3-5.7M4 4v4h4',
  headset: 'M4 13v-1a8 8 0 0 1 16 0v1M3 13h4v6H3zM17 13h4v6h-4z',
  pin: 'M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21zM12 12a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z',
  cash: 'M2 6h20v12H2zM12 14.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5zM6 9.5v5M18 9.5v5',
  phone: 'M7 2h10v20H7zM11 18h2',
  close: 'M5 5l14 14M19 5L5 19',
  check: 'M5 12.5l4.5 4.5L19 7',
  logout: 'M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 16l-4-4 4-4M6 12h10',
  store: 'M4 9l1.5-5h13L20 9M4 9h16v11H4zM10 20v-5h4v5',
  bell: 'M6 9a6 6 0 0 1 12 0c0 6 2.5 7.5 2.5 7.5h-17S6 15 6 9zM10 20a2 2 0 0 0 4 0',
  plus: 'M12 5v14M5 12h14',
  image: 'M3 4h18v16H3zM9 12a2 2 0 1 0 0-4 2 2 0 0 0 0 4zM21 16l-5-5-9 9',
  dashboard: 'M3 3h7v9H3zM14 3h7v5h-7zM14 12h7v9h-7zM3 16h7v5H3z',
  box: 'M3 7l9-4 9 4-9 4-9-4zM3 7v10l9 4 9-4V7M12 11v10',
  addCircle: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 8v8M8 12h8',
  chart: 'M4 20V10M10 20V4M16 20v-7M22 20H2',
  users: 'M9 11.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zM2.5 20c1-3.5 3.5-5.5 6.5-5.5s5.5 2 6.5 5.5M16 4.5a3.5 3.5 0 0 1 0 7M18 14.8c1.8.8 3 2.6 3.5 5.2',
} as const

export type IconName = keyof typeof PATHS

export function Icon({ name, className = 'size-5' }: { name: IconName; className?: string }) {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" className={`shrink-0 ${className}`}>
      <path d={PATHS[name]} />
    </svg>
  )
}
