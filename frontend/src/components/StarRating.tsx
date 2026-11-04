// Les étoiles sont décoratives : le lecteur d'écran lit le texte « Note : 4,5 sur 5 »
export function StarRating({ value }: { value: number }) {
  const label = `Note : ${value.toLocaleString('fr-SN')} sur 5`
  return (
    <span role="img" aria-label={label} title={label} className="text-safran">
      {[1, 2, 3, 4, 5].map((star) => (
        <span key={star} aria-hidden="true">
          {value >= star - 0.25 ? '★' : '☆'}
        </span>
      ))}
    </span>
  )
}
