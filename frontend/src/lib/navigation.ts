// Après connexion, on renvoie vers la page demandée (?retour=/panier). Sans contrôle, un lien piégé
// (?retour=https://site-pirate.example) ferait atterrir le client sur un faux site : « redirection ouverte ».
// On n'accepte donc qu'un chemin interne : commence par « / » mais pas par « // » ni « /\ ».
export function safeReturnPath(value: string | null, fallback = '/'): string {
  if (!value || !value.startsWith('/') || value.startsWith('//') || value.startsWith('/\\')) {
    return fallback
  }
  return value
}
