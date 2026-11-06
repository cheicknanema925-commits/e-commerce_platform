import { Link, useSearchParams } from 'react-router'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { Pagination } from '../../components/Pagination.tsx'
import { PageHeader, TableCard, td, th } from '../../components/admin/ui.tsx'
import { usePriceChanges } from '../../hooks/useSupport.ts'
import { formatDate, formatPrice } from '../../lib/format.ts'
import { priceChangeStateLabel } from '../../lib/labels.ts'

// Journal des changements de prix faits avec l'assistant : qui, quoi, avant, après, et ce qu'il en reste
export function AdminPriceChangesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page') ?? 0)
  const changes = usePriceChanges(page)

  return (
    <>
      <PageHeader
        title="Journal des prix"
        description={
          <>
            Chaque changement confirmé avec l’<Link to="/admin/assistant" className="underline underline-offset-2">assistant</Link> : ancien et
            nouveau prix, auteur, fin des promotions (les prix d’avant reviennent automatiquement).
          </>
        }
      />
      {changes.isError && <ErrorMessage error={changes.error} />}
      <TableCard title="Changements" count={changes.data?.totalElements}>
        <table className={`w-full text-sm transition-opacity ${changes.isPlaceholderData ? 'opacity-60' : ''}`}>
          <thead>
            <tr>
              <th scope="col" className={th}>Date</th>
              <th scope="col" className={th}>Produit</th>
              <th scope="col" className={th}>Type</th>
              <th scope="col" className={`${th} text-right`}>Avant</th>
              <th scope="col" className={`${th} text-right`}>Après</th>
              <th scope="col" className={th}>Fin</th>
              <th scope="col" className={th}>Auteur</th>
              <th scope="col" className={th}>État</th>
            </tr>
          </thead>
          <tbody>
            {changes.data?.content.map((change) => (
              <tr key={change.id} className="hover:bg-stone-50">
                <td className={td}>{formatDate(change.createdAt)}</td>
                <td className={td}>
                  <Link to={`/admin/produits/${change.productId}`} className="font-semibold hover:underline">
                    {change.productName}
                  </Link>
                </td>
                <td className={td}>{change.kind === 'PROMOTION' ? 'Promotion' : 'Nouveau prix'}</td>
                <td className={`${td} text-right tabular-nums`}>{formatPrice(change.oldPrice)}</td>
                <td className={`${td} text-right font-semibold tabular-nums`}>{formatPrice(change.newPrice)}</td>
                {/* endsAt = début du jour SUIVANT la promotion : on affiche le dernier jour inclus */}
                <td className={td}>{change.endsAt ? `jusqu’au ${formatDate(new Date(Date.parse(change.endsAt) - 1000).toISOString())}` : '—'}</td>
                <td className={td}>{change.authorName ?? '—'}</td>
                <td className={`${td} ${change.revertReason === null ? 'font-semibold text-kola' : 'text-stone-600'}`}>{priceChangeStateLabel(change)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {changes.data?.content.length === 0 && <p className="p-9 text-center text-stone-600">Aucun changement de prix pour le moment.</p>}
      </TableCard>
      {changes.data && (
        <Pagination page={changes.data.page} totalPages={changes.data.totalPages} onChange={(next) => setSearchParams({ page: String(next) })} />
      )}
    </>
  )
}
