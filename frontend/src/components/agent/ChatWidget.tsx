import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../../auth/useAuth.ts'
import { Icon } from '../Icon.tsx'
import { AgentChat } from './AgentChat.tsx'

const SUGGESTIONS = ['Avez-vous des sandales ?', 'Où en est ma dernière commande ?', 'Qu’y a-t-il dans mon panier ?', 'Mon colis est en retard']

// Bouton flottant de la boutique (clients connectés : l'API exige un jeton). Fenêtre NON modale : on peut continuer
// à parcourir la boutique pendant la conversation. Montée au premier clic, puis gardée (la conversation reste).
export function ChatWidget() {
  const { status } = useAuth()
  const [open, setOpen] = useState(false)
  const [mounted, setMounted] = useState(false)
  const button = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!open) {
      return
    }
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false)
        button.current?.focus()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open])

  if (status !== 'authenticated') {
    return null
  }

  function toggle() {
    setMounted(true)
    setOpen((current) => !current)
  }

  function close() {
    setOpen(false)
    button.current?.focus()
  }

  return (
    <>
      {mounted && (
        <section
          role="dialog"
          aria-modal="false"
          aria-labelledby="titre-assistant"
          hidden={!open}
          className="fixed inset-0 z-30 flex flex-col bg-white shadow-2xl sm:inset-auto sm:right-5 sm:bottom-24 sm:h-[min(620px,calc(100dvh-8rem))] sm:w-[400px] sm:rounded-2xl sm:border sm:border-stone-200"
        >
          <header className="flex items-center justify-between gap-3 rounded-t-2xl bg-brand-900 px-4 py-3 text-white">
            <h2 id="titre-assistant" className="flex items-center gap-2 font-semibold">
              <Icon name="chat" className="size-5 text-safran" />
              Assistant Shop Africa
            </h2>
            <button type="button" onClick={close} aria-label="Fermer l’assistant" className="grid size-8 place-items-center rounded-full hover:bg-white/10">
              <Icon name="close" className="size-4" />
            </button>
          </header>
          <div className="min-h-0 flex-1">
            <AgentChat
              kind="client"
              autoFocus={open}
              onNavigate={close}
              suggestions={SUGGESTIONS}
              intro={
                <p>
                  Bonjour ! Je peux chercher un produit, suivre vos commandes, préparer votre panier ou transmettre une demande au service
                  client. <b className="font-semibold text-stone-900">Je ne fais rien sans votre confirmation.</b>
                </p>
              }
            />
          </div>
        </section>
      )}
      <button
        ref={button}
        type="button"
        onClick={toggle}
        aria-expanded={open}
        className={`fixed right-5 bottom-5 z-30 items-center gap-2 rounded-full bg-brand-700 px-5 py-3.5 font-semibold text-white shadow-lg hover:bg-brand-900 ${open ? 'hidden sm:flex' : 'flex'}`}
      >
        <Icon name={open ? 'close' : 'chat'} className="size-5" />
        {open ? 'Fermer l’assistant' : 'Assistant'}
      </button>
    </>
  )
}
