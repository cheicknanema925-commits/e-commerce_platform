export type SseMessage = { event: string; data: string }

// Lecteur incrémental du format Server-Sent Events. Le texte arrive en morceaux quelconques (une ligne peut être coupée
// en deux) : on garde la fin incomplète en attente. Un message = des lignes « event: » et « data: », puis une ligne vide.
export function createSseParser(onMessage: (message: SseMessage) => void) {
  let pending = ''
  let event = 'message'
  let data: string[] = []

  function readLine(line: string) {
    if (line === '') {
      if (data.length > 0) {
        onMessage({ event, data: data.join('\n') })
      }
      event = 'message'
      data = []
      return
    }
    // « : texte » est un commentaire (souvent un battement de cœur pour garder la connexion ouverte)
    if (line.startsWith(':')) {
      return
    }
    const colon = line.indexOf(':')
    const field = colon === -1 ? line : line.slice(0, colon)
    const value = colon === -1 ? '' : line.slice(colon + 1).replace(/^ /, '')
    if (field === 'event') {
      event = value
    } else if (field === 'data') {
      data.push(value)
    }
  }

  return {
    push(chunk: string) {
      const lines = (pending + chunk).split('\n')
      pending = lines.pop() ?? ''
      lines.forEach((line) => readLine(line.replace(/\r$/, '')))
    },
    // Fin du flux : le dernier message n'est pas toujours suivi d'une ligne vide
    end() {
      if (pending !== '') {
        readLine(pending.replace(/\r$/, ''))
        pending = ''
      }
      readLine('')
    },
  }
}
