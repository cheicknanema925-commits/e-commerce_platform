import { describe, expect, it } from 'vitest'
import type { StoreSettings } from '../api/types.ts'
import { paymentAvailability, serviceFee } from './checkout.ts'

const settings: StoreSettings = {
  storeName: 'Shop Africa',
  supportEmail: 'contact@shopafrica.sn',
  onlinePaymentEnabled: true,
  codEnabled: true,
  codMaxAmount: 200_000,
  codFee: 500,
  codZones: [],
}

describe('paymentAvailability', () => {
  it('propose les deux modes sous le plafond des espèces', () => {
    const result = paymentAvailability(settings, 150_000)
    expect(result.ONLINE.available).toBe(true)
    expect(result.CASH_ON_DELIVERY.available).toBe(true)
  })

  it('refuse les espèces au-delà du plafond, avec la raison', () => {
    const result = paymentAvailability(settings, 250_000)
    expect(result.CASH_ON_DELIVERY.available).toBe(false)
    expect(result.CASH_ON_DELIVERY.reason).toMatch(/^Disponible jusqu’à 200\s000\sF\sCFA/u)
  })

  it('suit les interrupteurs des paramètres', () => {
    const result = paymentAvailability({ ...settings, onlinePaymentEnabled: false, codEnabled: false }, 1000)
    expect(result.ONLINE.available).toBe(false)
    expect(result.CASH_ON_DELIVERY.available).toBe(false)
  })
})

describe('serviceFee', () => {
  it('ne s’applique qu’au paiement en espèces', () => {
    expect(serviceFee(settings, 'CASH_ON_DELIVERY')).toBe(500)
    expect(serviceFee(settings, 'ONLINE')).toBe(0)
  })
})
