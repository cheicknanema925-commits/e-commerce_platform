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
    <div className="mx-auto max-w-md">
      <h1 className="text-3xl font-bold tracking-tight">Connexion</h1>
      <form onSubmit={onSubmit} className="mt-6 flex flex-col gap-4 rounded-lg border border-stone-200 bg-white p-6">
        {error !== null && <ErrorMessage error={error} />}
        <FormField name="email" label="Email" type="email" autoComplete="email" required maxLength={255} />
        <FormField name="password" label="Mot de passe" type="password" autoComplete="current-password" required maxLength={128} />
        <button
          type="submit"
          disabled={pending}
          className="rounded-md bg-brand-700 px-4 py-2.5 font-medium text-white hover:bg-brand-800 disabled:opacity-60"
        >
          {pending ? 'Connexion…' : 'Se connecter'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-stone-600">
        Pas encore de compte ?{' '}
        <Link to={`/inscription?${searchParams}`} className="font-medium text-brand-700 hover:underline">
          Créer un compte
        </Link>
      </p>
    </div>
  )
}
