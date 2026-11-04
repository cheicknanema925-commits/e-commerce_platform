import type { ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router'
import type { ProductSort } from '../api/catalog.ts'
import type { Category } from '../api/types.ts'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { Icon, type IconName } from '../components/Icon.tsx'
import { Pagination } from '../components/Pagination.tsx'
import { ProductCard } from '../components/ProductCard.tsx'
import { useCategories, useProducts } from '../hooks/useCatalog.ts'
import { categoryVisual } from '../lib/categories.ts'

const SORTS: { value: ProductSort; label: string }[] = [
  { value: 'id,desc', label: 'Nouveautés' },
  { value: 'price,asc', label: 'Prix croissant' },
  { value: 'price,desc', label: 'Prix décroissant' },
  { value: 'name,asc', label: 'Nom (A → Z)' },
]

const SERVICES: { icon: IconName; title: string; text: string }[] = [
  { icon: 'truck', title: 'Livraison à domicile', text: 'Partout au Sénégal, avec suivi étape par étape' },
  { icon: 'lock', title: 'Payez comme vous voulez', text: 'Wave, Orange Money, carte ou espèces à la livraison' },
  { icon: 'pin', title: 'Livreur guidé', text: 'Partagez votre position GPS à la commande' },
  { icon: 'headset', title: 'Service client', text: 'Une question ? Nous répondons' },
]

// Les filtres vivent dans l'URL (?q=bazin&category=mode&page=1) : on peut partager le lien,
// et les boutons Précédent / Suivant du navigateur fonctionnent
export function CatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = {
    q: searchParams.get('q') ?? undefined,
    category: searchParams.get('category') ?? undefined,
    sort: (searchParams.get('sort') as ProductSort | null) ?? undefined,
    page: Number(searchParams.get('page') ?? 0),
  }
  const products = useProducts(filters)
  const categories = useCategories()
  // Accueil = aucune recherche, aucun rayon, première page : on montre l'étal, les services et les rayons
  const home = !filters.q && !filters.category && filters.page === 0
  const category = categories.data?.find((c) => c.slug === filters.category)

  // Changer un filtre ramène à la première page
  function updateFilter(name: string, value: string) {
    const next = new URLSearchParams(searchParams)
    if (value) {
      next.set(name, value)
    } else {
      next.delete(name)
    }
    next.delete('page')
    setSearchParams(next)
  }

  function goToPage(page: number) {
    const next = new URLSearchParams(searchParams)
    next.set('page', String(page))
    setSearchParams(next)
    document.getElementById('produits')?.scrollIntoView()
  }

  const title = filters.q ? 'Résultats de recherche' : (category?.name ?? (home ? 'Nos produits' : 'Tous les produits'))
  return (
    <>
      {home && (
        <>
          <Stall />
          {/* gap-px sur fond gris : les traits de séparation s'adaptent seuls au nombre de colonnes */}
          <ul aria-label="Nos services" className="mt-5 grid gap-px overflow-hidden rounded-lg border border-stone-200 bg-stone-200 sm:grid-cols-2 lg:grid-cols-4">
            {SERVICES.map((service) => (
              <li key={service.title} className="flex items-start gap-3.5 bg-white px-5 py-4 text-sm text-stone-600">
                <Icon name={service.icon} className="size-[26px] text-brand-700" />
                <div>
                  <b className="block text-[15px] font-semibold text-stone-900">{service.title}</b>
                  {service.text}
                </div>
              </li>
            ))}
          </ul>

          {categories.data && categories.data.length > 0 && (
            <section aria-labelledby="rayons">
              <SectionTitle id="rayons" title="Les rayons" subtitle="Choisissez un rayon pour voir ses produits." />
              <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
                {categories.data.map((c) => (
                  <li key={c.id}>
                    <Aisle category={c} />
                  </li>
                ))}
              </ul>
            </section>
          )}
        </>
      )}

      <section aria-labelledby="titre-produits" id="produits" className="scroll-mt-40">
        <SectionTitle
          id="titre-produits"
          title={title}
          subtitle={filters.q ? `Pour « ${filters.q} »` : home ? 'Les derniers produits arrivés dans la boutique.' : undefined}
          action={
            !home && (
              <Link to="/" className="text-sm font-semibold whitespace-nowrap text-brand-700 underline underline-offset-3">
                Voir tous les produits
              </Link>
            )
          }
        />

        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div role="group" aria-label="Filtrer par rayon" className="flex flex-wrap gap-2">
            <Chip pressed={!filters.category} onClick={() => updateFilter('category', '')}>
              Tous
            </Chip>
            {categories.data?.map((c) => (
              <Chip key={c.id} pressed={filters.category === c.slug} onClick={() => updateFilter('category', c.slug)}>
                {c.name}
              </Chip>
            ))}
          </div>
          <label className="flex items-center gap-2 text-sm text-stone-600">
            Trier par
            <select
              value={filters.sort ?? 'id,desc'}
              onChange={(event) => updateFilter('sort', event.target.value)}
              className="rounded-lg border border-stone-200 bg-white px-2.5 py-2 text-stone-900"
            >
              {SORTS.map((sort) => (
                <option key={sort.value} value={sort.value}>
                  {sort.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        {products.isPending && <p className="text-stone-600">Chargement du catalogue…</p>}
        {products.isError && <ErrorMessage error={products.error} />}
        {products.data && (
          <>
            {/* aria-live : le nombre de résultats est annoncé quand les filtres changent */}
            <p aria-live="polite" className="mb-3.5 text-sm text-stone-600">
              {products.data.totalElements} produit{products.data.totalElements > 1 ? 's' : ''}
              {filters.q && ` pour « ${filters.q} »`}
            </p>
            {products.data.content.length === 0 ? (
              <div className="rounded-lg border border-dashed border-stone-300 bg-white p-10 text-center text-stone-600">
                <b className="text-stone-900">Aucun produit ne correspond à votre recherche.</b>
                <p className="mt-1">Vérifiez l'orthographe ou essayez un mot plus court.</p>
                <Link to="/" className="mt-2.5 inline-block font-semibold text-brand-700 underline">
                  Afficher tous les produits
                </Link>
              </div>
            ) : (
              <ul className={`grid grid-cols-2 gap-2.5 sm:gap-4 md:grid-cols-3 lg:grid-cols-4 ${products.isPlaceholderData ? 'opacity-60' : ''}`}>
                {products.data.content.map((product) => (
                  <li key={product.id}>
                    <ProductCard product={product} />
                  </li>
                ))}
              </ul>
            )}
            <Pagination page={products.data.page} totalPages={products.data.totalPages} onChange={goToPage} />
          </>
        )}
      </section>
    </>
  )
}

function SectionTitle({ id, title, subtitle, action }: { id: string; title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="mt-14 mb-4.5 flex items-baseline justify-between gap-4">
      <div>
        <h2 id={id} className="text-[clamp(26px,3vw,34px)] leading-tight font-bold tracking-tight">
          {title}
        </h2>
        {subtitle && <p className="mt-1.5 text-[15px] text-stone-600">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

function Chip({ pressed, onClick, children }: { pressed: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <button
      type="button"
      aria-pressed={pressed}
      onClick={onClick}
      className={`rounded-full border px-4 py-1.5 text-sm ${pressed ? 'border-brand-700 bg-brand-700 text-white' : 'border-stone-200 bg-white text-stone-600 hover:border-stone-400'}`}
    >
      {children}
    </button>
  )
}

function Aisle({ category }: { category: Category }) {
  const visual = categoryVisual(category.slug)
  return (
    <Link
      to={`/?category=${category.slug}`}
      className={`flex min-h-[132px] flex-col items-start justify-between gap-7 rounded-lg p-4 transition hover:-translate-y-0.5 ${visual.tint}`}
    >
      <span aria-hidden="true" className="text-[34px] leading-none">
        {visual.emoji}
      </span>
      <span>
        <b className="font-semibold">{category.name}</b>
        <small className="block text-[13px] opacity-75">Voir les produits</small>
      </span>
    </Link>
  )
}

// L'étal du marché : auvent rayé safran et blanc, texte d'accueil, motif inspiré du tissu wax
function Stall() {
  return (
    <section aria-labelledby="accueil" className="overflow-hidden rounded-[18px] bg-brand-700 text-white">
      <svg viewBox="0 0 1200 46" preserveAspectRatio="none" aria-hidden="true" className="block h-[46px] w-full">
        <defs>
          <pattern id="rayures" width="120" height="46" patternUnits="userSpaceOnUse">
            <rect width="60" height="46" fill="#F2A900" />
            <rect x="60" width="60" height="46" fill="#ffffff" />
          </pattern>
        </defs>
        <path
          d={`M0 0H1200V26${Array.from({ length: 20 }, (_, i) => ` Q${1170 - i * 60} 46 ${1140 - i * 60} 26`).join('')}Z`}
          fill="url(#rayures)"
        />
      </svg>
      <div className="grid lg:grid-cols-[1.15fr_0.85fr]">
        <div className="px-6 pt-6 pb-9 sm:px-12 sm:pt-9 sm:pb-13">
          <h1 id="accueil" className="mb-5 max-w-[11ch] text-[clamp(40px,5.6vw,72px)] leading-[0.98] font-extrabold tracking-tight">
            Le marché du quotidien, livré chez vous.
          </h1>
          <p className="mb-7 max-w-[46ch] text-lg leading-relaxed font-light text-brand-100">
            Mode, maison, électronique et beauté : commandez en quelques clics, payez en ligne ou en espèces au livreur, et
            suivez votre colis jusqu'à votre porte.
          </p>
          <a href="#produits" className="inline-flex items-center gap-2.5 rounded-full bg-safran px-6.5 py-3.5 font-bold text-stone-900 hover:bg-safran-light">
            Voir les produits
          </a>
          <ul aria-label="Moyens de paiement acceptés" className="mt-7 flex flex-wrap gap-2">
            {['Wave', 'Orange Money', 'Carte bancaire', 'Espèces à la livraison'].map((method) => (
              <li key={method} className="rounded-full border border-white/25 px-3.5 py-1.5 text-[13px]">
                {method}
              </li>
            ))}
          </ul>
        </div>
        <div className="relative min-h-[220px] lg:min-h-[360px]">
          <svg viewBox="0 0 400 400" preserveAspectRatio="xMidYMid slice" aria-hidden="true" className="absolute inset-0 size-full">
            <defs>
              <pattern id="wax" width="100" height="100" patternUnits="userSpaceOnUse">
                <rect width="100" height="100" fill="#141C55" />
                <circle cx="50" cy="50" r="38" fill="#F2A900" />
                <circle cx="50" cy="50" r="27" fill="#B5123F" />
                <circle cx="50" cy="50" r="15" fill="#141C55" />
                <circle cx="50" cy="50" r="6" fill="#F2A900" />
                <path d="M0 0 L16 0 L0 16Z M100 0 L84 0 L100 16Z M0 100 L16 100 L0 84Z M100 100 L84 100 L100 84Z" fill="#0E7A5C" />
                <circle cx="0" cy="50" r="5" fill="#fff" />
                <circle cx="100" cy="50" r="5" fill="#fff" />
                <circle cx="50" cy="0" r="5" fill="#fff" />
                <circle cx="50" cy="100" r="5" fill="#fff" />
              </pattern>
            </defs>
            <rect width="400" height="400" fill="url(#wax)" />
          </svg>
          <aside aria-labelledby="offre" className="absolute right-7 bottom-7 left-7 max-w-[300px] rounded-lg bg-white px-5 py-4.5 text-stone-900 shadow-[0_14px_30px_#0b10404d]">
            <small className="text-[13px] font-semibold text-bissap">Payez à la réception</small>
            <h2 id="offre" className="mt-1 mb-1.5 text-[22px] leading-tight font-bold tracking-tight">
              Espèces à la livraison
            </h2>
            <p className="mb-3 text-sm text-stone-600">Vous payez le livreur quand il vous remet le colis.</p>
            <Link to="/?sort=id,desc" className="text-sm font-semibold text-brand-700 underline underline-offset-3">
              Voir les nouveautés
            </Link>
          </aside>
        </div>
      </div>
    </section>
  )
}
