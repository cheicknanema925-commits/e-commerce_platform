import { type FormEvent } from 'react'
import { useSubscribe } from '../hooks/useShop.ts'
import { useToast } from '../lib/toast.ts'
import { ErrorMessage } from './ErrorMessage.tsx'

// Bloc safran de la maquette. La réponse est la même que l'adresse soit nouvelle ou déjà inscrite
export function NewsletterSignup() {
  const subscribe = useSubscribe()
  const toast = useToast()

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const email = String(new FormData(form).get('email') ?? '').trim()
    subscribe.mutate(email, {
      onSuccess: (result) => {
        toast(result.message)
        form.reset()
      },
    })
  }

  return (
    <section aria-labelledby="infolettre" className="mt-16 grid items-center gap-7 rounded-[18px] bg-safran px-6 py-7 sm:px-11 sm:py-9 lg:grid-cols-[1fr_auto]">
      <div>
        <h2 id="infolettre" className="text-[clamp(24px,2.8vw,32px)] leading-tight font-bold tracking-tight">
          Recevez les promotions de la semaine
        </h2>
        <p className="mt-1.5 max-w-[48ch] text-[15px]">Un e-mail par semaine avec les meilleures remises. Désinscription en un clic.</p>
      </div>
      <form onSubmit={onSubmit} className="flex flex-col gap-2 sm:flex-row">
        <label className="flex-1">
          <span className="sr-only">Adresse e-mail</span>
          <input
            name="email"
            type="email"
            required
            maxLength={255}
            autoComplete="email"
            placeholder="Votre adresse e-mail"
            className="w-full rounded-full bg-white px-4.5 py-3.5 outline-none focus:ring-3 focus:ring-stone-900/30 sm:min-w-[260px]"
          />
        </label>
        <button type="submit" disabled={subscribe.isPending} className="rounded-full bg-stone-900 px-5.5 py-3.5 font-semibold whitespace-nowrap text-white disabled:opacity-60">
          {subscribe.isPending ? 'Inscription…' : 'S’inscrire'}
        </button>
      </form>
      {subscribe.isError && (
        <div className="lg:col-span-2">
          <ErrorMessage error={subscribe.error} />
        </div>
      )}
    </section>
  )
}
