import { describe, expect, it } from 'vitest'
import { geolocationErrorMessage, osmEmbedUrl, roundCoordinate } from './location.ts'

describe('roundCoordinate', () => {
  it('garde 6 décimales, la précision de la base', () => {
    expect(roundCoordinate(14.69281234)).toBe(14.692812)
    expect(roundCoordinate(-17.44674999)).toBe(-17.44675)
  })
})

describe('osmEmbedUrl', () => {
  it('centre la carte sur le point avec un marqueur', () => {
    const url = osmEmbedUrl({ latitude: 14.6928, longitude: -17.4467 })
    expect(url).toBe(
      'https://www.openstreetmap.org/export/embed.html?bbox=-17.450700,14.688800,-17.442700,14.696800&layer=mapnik&marker=14.6928,-17.4467',
    )
  })
})

describe('geolocationErrorMessage', () => {
  it('explique un refus et propose de continuer sans position', () => {
    expect(geolocationErrorMessage(1)).toContain('continuez sans')
    expect(geolocationErrorMessage(3)).toContain('trop de temps')
    expect(geolocationErrorMessage(2)).toContain('introuvable')
  })
})
