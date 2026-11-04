import { useNavigate } from 'react-router'
import type { Product } from '../api/types.ts'
import { useAuth } from '../auth/useAuth.ts'
import { useFavoriteIds, useToggleFavorite } from '../hooks/useShop.ts'
import { useToast } from '../lib/toast.ts'
import { Icon } from './Icon.tsx'

// Cœur des favoris. aria-pressed : un lecteur d'écran annonce « bouton, enfoncé » quand le produit est en favori
export function FavoriteButton({ product, large = false }: { product: Pick<Product, 'id' | 'name'>; large?: boolean }) {
  const { status } = useAuth()
  const ids = useFavoriteIds()
  const toggle = useToggleFavorite()
  const toast = useToast()
  const navigate = useNavigate()
  const favorite = ids.data?.includes(product.id) ?? false

  function onClick() {
    if (status !== 'authenticated') {
      navigate(`/connexion?retour=${encodeURIComponent(`/produits/${product.id}`)}`)
      return
    }
    toggle.mutate(
      { productId: product.id, favorite: !favorite },
      {
        onSuccess: () => toast(favorite ? 'Retiré des favoris' : 'Ajouté aux favoris'),
        onError: (error) => toast(error.message),
      },
    )
  }

  if (large) {
    return (
      <button
        type="button"
        aria-pressed={favorite}
        onClick={onClick}
        className="inline-flex items-center gap-2 rounded-full border-2 border-stone-200 px-5 py-3 font-semibold hover:border-bissap aria-pressed:border-bissap aria-pressed:text-bissap"
      >
        <Icon name="heart" className={`size-5 ${favorite ? 'fill-bissap text-bissap' : ''}`} />
        {favorite ? 'Dans vos favoris' : 'Ajouter aux favoris'}
      </button>
    )
  }
  return (
    <button
      type="button"
      aria-pressed={favorite}
      aria-label={`${favorite ? 'Retirer des favoris' : 'Ajouter aux favoris'} : ${product.name}`}
      onClick={onClick}
      className="relative z-10 grid size-9 place-items-center rounded-full bg-white shadow-sm hover:scale-105"
    >
      <Icon name="heart" className={`size-[19px] ${favorite ? 'fill-bissap text-bissap' : 'text-stone-900'}`} />
    </button>
  )
}
