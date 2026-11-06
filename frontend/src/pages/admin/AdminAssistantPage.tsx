import { Link } from 'react-router'
import { AgentChat } from '../../components/agent/AgentChat.tsx'
import { PageHeader } from '../../components/admin/ui.tsx'

const SUGGESTIONS = [
  'Résume les ventes de ce mois',
  'Quels produits sont en stock faible ?',
  'Prépare une promotion de 15 % sur la Mode jusqu’à dimanche',
  'Annule le dernier changement de prix',
]

// Assistant du back-office : chiffres calculés en base, changements de prix proposés puis confirmés d'un clic
export function AdminAssistantPage() {
  return (
    <>
      <PageHeader
        title="Assistant IA"
        description="Posez vos questions sur les ventes, préparez une promotion ou un nouveau prix. Rien ne change sans votre confirmation."
      />
      <section aria-label="Conversation avec l’assistant" className="h-[calc(100dvh-210px)] min-h-[420px] overflow-hidden rounded-lg border border-stone-200 bg-white">
        <AgentChat
          kind="admin"
          autoFocus
          suggestions={SUGGESTIONS}
          intro={
            <p>
              Les limites de la boutique s’appliquent toujours : remise de 70 % au maximum, date de fin obligatoire pour une promotion
              (90 jours au plus), 50 produits par changement. Chaque changement est inscrit au{' '}
              <Link to="/admin/prix" className="font-semibold text-brand-700 underline underline-offset-2">
                journal des prix
              </Link>{' '}
              et peut être annulé.
            </p>
          }
        />
      </section>
    </>
  )
}
