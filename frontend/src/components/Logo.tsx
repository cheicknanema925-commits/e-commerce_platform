// Logo de la maquette : l'auvent rayé de l'étal au-dessus d'un colis. « dark » = version pour fond indigo.
export function LogoMark({ dark = false, size = 34 }: { dark?: boolean; size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 34 34" aria-hidden="true" className="shrink-0">
      <rect width="34" height="34" rx="9" fill={dark ? '#fff' : '#1E2A78'} />
      <path d="M5 13 Q8.5 17 12 13 Q15.5 17 19 13 Q22.5 17 26 13 Q29 16.5 29 13 V11 H5 Z" fill="#F2A900" />
      <rect x="9" y="17" width="16" height="10" rx="2" fill="none" stroke={dark ? '#1E2A78' : '#fff'} strokeWidth="2" />
      <circle cx="17" cy="22" r="2" fill="#F2A900" />
    </svg>
  )
}

export function Logo({ dark = false }: { dark?: boolean }) {
  return (
    <span className={`flex items-center gap-2.5 text-2xl leading-none font-extrabold tracking-tight whitespace-nowrap ${dark ? 'text-white' : 'text-stone-900'}`}>
      <LogoMark dark={dark} />
      <span>
        Shop <span className={dark ? 'text-safran' : 'text-brand-700'}>Africa</span>
      </span>
    </span>
  )
}
