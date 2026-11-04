import { useSearchParams } from 'react-router'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { ExportButton } from '../../components/admin/ExportButton.tsx'
import { Kpi, KpiStrip, PageHeader, TableCard, td, th } from '../../components/admin/ui.tsx'
import { useSubscribers } from '../../hooks/useAdmin.ts'
import { formatDate, formatNumber } from '../../lib/format.ts'

export function AdminNewsletterPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page') ?? 0)
  const subscribers = useSubscribers(page)

  return (
    <>
      <PageHeader
        title="Infolettre"
        description="Les personnes inscrites aux promotions de la semaine. Chacune peut se désinscrire en un clic."
        actions={<ExportButton name="newsletter" label="Exporter les abonnés" />}
      />
      <KpiStrip>
        <Kpi label="Abonnés" value={formatNumber(subscribers.data?.totalElements ?? 0)} detail="Inscrits depuis l’accueil de la boutique" />
      </KpiStrip>
      <p className="rounded-lg border border-stone-200 bg-white p-4 text-sm text-stone-600">
        L’envoi réel des emails (campagnes) sera branché avec un service d’emailing ; aujourd’hui, le message de bienvenue
        avec le lien de désinscription est écrit dans les logs du serveur.
      </p>
      {subscribers.isError && <ErrorMessage error={subscribers.error} />}
      <TableCard title="Abonnés" count={subscribers.data?.totalElements}>
        <table className="w-full text-sm">
          <thead>
            <tr>
              <th scope="col" className={th}>Email</th>
              <th scope="col" className={th}>Inscrit le</th>
            </tr>
          </thead>
          <tbody>
            {subscribers.data?.content.map((subscriber) => (
              <tr key={subscriber.id} className="hover:bg-stone-50">
                <td className={td}>{subscriber.email}</td>
                <td className={td}>{formatDate(subscriber.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {subscribers.data?.content.length === 0 && <p className="p-9 text-center text-stone-600">Aucun abonné pour l'instant.</p>}
      </TableCard>
      {subscribers.data && (
        <Pagination page={subscribers.data.page} totalPages={subscribers.data.totalPages} onChange={(next) => setSearchParams({ page: String(next) })} />
      )}
    </>
  )
}
