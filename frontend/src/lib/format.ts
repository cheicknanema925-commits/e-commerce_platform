// Formats d'affichage pour le Sénégal : francs CFA sans centimes, dates en français

const priceFormatter = new Intl.NumberFormat('fr-SN', {
  style: 'currency',
  currency: 'XOF',
  maximumFractionDigits: 0,
})

const dateFormatter = new Intl.DateTimeFormat('fr-SN', { dateStyle: 'long' })

export function formatPrice(amount: number): string {
  return priceFormatter.format(amount)
}

export function formatDate(isoDate: string): string {
  return dateFormatter.format(new Date(isoDate))
}

const numberFormatter = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 })

// Nombre seul, avec séparateur de milliers (l'unité « FCFA » est écrite à côté, en plus petit)
export function formatNumber(value: number): string {
  return numberFormatter.format(value)
}
