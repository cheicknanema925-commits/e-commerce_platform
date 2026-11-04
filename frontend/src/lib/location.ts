// Position GPS de livraison : arrondie à 6 décimales (~10 cm), comme la colonne NUMERIC(9, 6) de la base
export type Position = { latitude: number; longitude: number }

export function roundCoordinate(value: number): number {
  return Math.round(value * 1_000_000) / 1_000_000
}

// Carte OpenStreetMap intégrée (iframe officielle export/embed) : petite zone autour du point + marqueur.
// La position est alors envoyée à openstreetmap.org pour afficher la carte (dit au client à côté de la carte).
export function osmEmbedUrl({ latitude, longitude }: Position, delta = 0.004): string {
  const bbox = [longitude - delta, latitude - delta, longitude + delta, latitude + delta].map((v) => v.toFixed(6)).join(',')
  return `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${latitude},${longitude}`
}

export function osmLink({ latitude, longitude }: Position): string {
  return `https://www.openstreetmap.org/?mlat=${latitude}&mlon=${longitude}#map=17/${latitude}/${longitude}`
}

// Codes de GeolocationPositionError : 1 = refus, 2 = position introuvable, 3 = délai dépassé
export function geolocationErrorMessage(code: number): string {
  switch (code) {
    case 1:
      return "Vous n'avez pas autorisé l'accès à votre position. Autorisez-le dans votre navigateur, ou continuez sans : l'adresse suffit."
    case 3:
      return 'La position met trop de temps à arriver. Réessayez à l’extérieur ou près d’une fenêtre, ou continuez sans.'
    default:
      return 'Position introuvable pour le moment. Vérifiez que la localisation de votre téléphone est activée, ou continuez sans.'
  }
}
