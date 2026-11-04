import type { PaymentMethod, StoreSettings } from '../api/types.ts'
import { formatPrice } from './format.ts'

export type Availability = { available: boolean; reason?: string }

// Ce que la page de commande propose, d'après les paramètres publics de la boutique.
// Simple confort d'affichage : le serveur revérifie tout au moment de la commande (zones comprises).
export function paymentAvailability(settings: StoreSettings | undefined, cartTotal: number): Record<PaymentMethod, Availability> {
  if (!settings) {
    return { ONLINE: { available: true }, CASH_ON_DELIVERY: { available: true } }
  }
  const cash: Availability = !settings.codEnabled
    ? { available: false, reason: 'Momentanément indisponible.' }
    : settings.codMaxAmount !== null && cartTotal > settings.codMaxAmount
      ? { available: false, reason: `Disponible jusqu’à ${formatPrice(settings.codMaxAmount)} de commande.` }
      : { available: true }
  return {
    ONLINE: settings.onlinePaymentEnabled ? { available: true } : { available: false, reason: 'Momentanément indisponible.' },
    CASH_ON_DELIVERY: cash,
  }
}

// Frais de service : uniquement pour un paiement en espèces
export function serviceFee(settings: StoreSettings | undefined, method: PaymentMethod): number {
  return method === 'CASH_ON_DELIVERY' ? (settings?.codFee ?? 0) : 0
}
