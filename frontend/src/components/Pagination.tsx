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
    'rounded-md border border-stone-300 bg-white px-4 py-2 text-sm font-medium hover:bg-stone-100 disabled:cursor-not-allowed disabled:opacity-50'
  return (
    <nav aria-label="Pagination" className="mt-8 flex items-center justify-center gap-4">
      <button type="button" className={buttonClass} disabled={page === 0} onClick={() => onChange(page - 1)}>
        Précédente
      </button>
      <span className="text-sm text-stone-700">
        Page {page + 1} sur {totalPages}
      </span>
      <button
        type="button"
        className={buttonClass}
        disabled={page + 1 >= totalPages}
        onClick={() => onChange(page + 1)}
      >
        Suivante
      </button>
    </nav>
  )
}
