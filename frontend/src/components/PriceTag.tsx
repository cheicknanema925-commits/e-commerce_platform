import { formatPrice } from '../lib/format.ts'

// L'étiquette de prix du marché (forme découpée + trou), le signe distinctif de la maquette
export function PriceTag({ amount, size = 'md' }: { amount: number; size?: 'md' | 'lg' }) {
  return (
    <span
      className={`relative inline-flex items-center bg-safran font-extrabold tracking-tight text-stone-900 [clip-path:polygon(12px_0,100%_0,100%_100%,12px_100%,0_50%)] before:absolute before:top-1/2 before:left-[9px] before:size-1.5 before:-translate-y-1/2 before:rounded-full before:bg-white ${
        size === 'lg' ? 'py-2 pr-4 pl-7 text-2xl' : 'py-1 pr-3 pl-[22px] text-[17px]'
      }`}
    >
      {formatPrice(amount)}
    </span>
  )
}
