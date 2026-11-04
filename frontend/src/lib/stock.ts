// Message de stock affiché au client : rupture, presque épuisé (5 ou moins), disponible
export function stockText(stock: number): { text: string; className: string } {
  if (stock === 0) {
    return { text: 'Rupture de stock', className: 'text-bissap' }
  }
  if (stock <= 5) {
    return { text: `Plus que ${stock} en stock`, className: 'text-safran-ink' }
  }
  return { text: 'En stock', className: 'text-kola' }
}
