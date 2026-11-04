import { type FormEvent, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { FormField } from '../components/FormField.tsx'
import { useAuth } from '../auth/useAuth.ts'
import { safeReturnPath } from '../lib/navigation.ts'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [error, setError] = useState<unknown>(null)
  const [pending, setPending] = useState(false)

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setPending(true)
    setError(null)
    try {
      await login(String(form.get('email')), String(form.get('password')))
      navigate(safeReturnPath(searchParams.get('retour')), { replace: true })
    } catch (e) {
      setError(e)
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="mx-auto max-w-[420px] py-6">
      <h1 className="text-[28px] font-bold tracking-tight">Connexion</h1>
      <p className="mt-1.5 text-sm text-stone-600">Connectez-vous pour suivre vos commandes et payer en quelques clics.</p>
      <form onSubmit={onSubmit} className="mt-5 flex flex-col gap-4 rounded-[18px] border border-stone-200 bg-white p-6 sm:p-7">
        {error !== null && <ErrorMessage error={error} />}
        <FormField name="email" label="Email" type="email" autoComplete="email" required maxLength={255} />
        <FormField name="password" label="Mot de passe" type="password" autoComplete="current-password" required maxLength={128} />
        <button
          type="submit"
          disabled={pending}
          className="mt-1 rounded-full bg-safran py-3.5 font-bold text-stone-900 hover:bg-safran-light disabled:opacity-60"
        >
          {pending ? 'Connexion…' : 'Se connecter'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-stone-600">
        Pas encore de compte ?{' '}
        <Link to={`/inscription?${searchParams}`} className="font-semibold text-brand-700 underline underline-offset-3">
          Créer un compte
        </Link>
      </p>
    </div>
  )
}
