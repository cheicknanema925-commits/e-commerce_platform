import { type FormEvent, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { useAuth } from '../auth/useAuth.ts'
import { ErrorMessage } from '../components/ErrorMessage.tsx'
import { FormField } from '../components/FormField.tsx'
import { fieldErrors } from '../lib/forms.ts'
import { safeReturnPath } from '../lib/navigation.ts'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [error, setError] = useState<unknown>(null)
  const [pending, setPending] = useState(false)
  const errors = fieldErrors(error)

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setPending(true)
    setError(null)
    try {
      await register({
        fullName: String(form.get('fullName')),
        email: String(form.get('email')),
        password: String(form.get('password')),
      })
      navigate(safeReturnPath(searchParams.get('retour')), { replace: true })
    } catch (e) {
      setError(e)
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <h1 className="text-3xl font-bold tracking-tight">Créer un compte</h1>
      {/* Les contrôles du navigateur (required, minLength…) aident le client ; l'API revérifie tout de toute façon */}
      <form onSubmit={onSubmit} className="mt-6 flex flex-col gap-4 rounded-lg border border-stone-200 bg-white p-6">
        {error !== null && Object.keys(errors).length === 0 && <ErrorMessage error={error} />}
        <FormField name="fullName" label="Nom complet" autoComplete="name" required maxLength={150} error={errors.fullName} />
        <FormField name="email" label="Email" type="email" autoComplete="email" required maxLength={255} error={errors.email} />
        <FormField
          name="password"
          label="Mot de passe"
          type="password"
          autoComplete="new-password"
          required
          minLength={12}
          maxLength={72}
          hint="12 caractères minimum. Une phrase facile à retenir est plus sûre qu'un mot compliqué."
          error={errors.password}
        />
        <button
          type="submit"
          disabled={pending}
          className="rounded-md bg-brand-700 px-4 py-2.5 font-medium text-white hover:bg-brand-800 disabled:opacity-60"
        >
          {pending ? 'Création…' : 'Créer mon compte'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-stone-600">
        Déjà un compte ?{' '}
        <Link to={`/connexion?${searchParams}`} className="font-medium text-brand-700 hover:underline">
          Se connecter
        </Link>
      </p>
    </div>
  )
}
