type PaginationProps = {
  page: number
  totalPages: number
  onChange: (page: number) => void
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }
  const buttonClass =
    'rounded-full border border-stone-200 bg-white px-5 py-2 text-sm font-semibold hover:border-brand-700 hover:text-brand-700 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:border-stone-200 disabled:hover:text-current'
  return (
    <nav aria-label="Pagination" className="mt-8 flex items-center justify-center gap-4">
      <button type="button" className={buttonClass} disabled={page === 0} onClick={() => onChange(page - 1)}>
        Précédente
      </button>
      <span className="text-sm text-stone-600">
        Page {page + 1} sur {totalPages}
      </span>
      <button type="button" className={buttonClass} disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        Suivante
      </button>
    </nav>
  )
}
