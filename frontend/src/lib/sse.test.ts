import { describe, expect, it } from 'vitest'
import { createSseParser, type SseMessage } from './sse.ts'

function parse(chunks: string[]): SseMessage[] {
  const messages: SseMessage[] = []
  const parser = createSseParser((message) => messages.push(message))
  chunks.forEach((chunk) => parser.push(chunk))
  parser.end()
  return messages
}

describe('createSseParser', () => {
  it('lit les messages au format de Spring (« event:token », sans espace)', () => {
    expect(parse(['event:token\ndata:{"text":"Bon"}\n\nevent:done\ndata:{}\n\n'])).toEqual([
      { event: 'token', data: '{"text":"Bon"}' },
      { event: 'done', data: '{}' },
    ])
  })

  it('recolle une ligne coupée entre deux morceaux', () => {
    expect(parse(['event:tok', 'en\ndata:{"te', 'xt":"jour"}\n', '\n'])).toEqual([{ event: 'token', data: '{"text":"jour"}' }])
  })

  it('accepte les fins de ligne Windows, les commentaires et un dernier message sans ligne vide', () => {
    expect(parse([': ping\r\n\r\nevent: warning\r\ndata: {"message":"Attention"}'])).toEqual([
      { event: 'warning', data: '{"message":"Attention"}' },
    ])
  })

  it('joint plusieurs lignes « data » et utilise « message » sans « event »', () => {
    expect(parse(['data:ligne 1\ndata:ligne 2\n\n'])).toEqual([{ event: 'message', data: 'ligne 1\nligne 2' }])
  })
})
