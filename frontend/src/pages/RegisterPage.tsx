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
    <div className="mx-auto max-w-[420px] py-6">
      <h1 className="text-[28px] font-bold tracking-tight">Créer un compte</h1>
      <p className="mt-1.5 text-sm text-stone-600">Un compte suffit pour commander, payer et suivre vos livraisons.</p>
      {/* Les contrôles du navigateur (required, minLength…) aident le client ; l'API revérifie tout de toute façon */}
      <form onSubmit={onSubmit} className="mt-5 flex flex-col gap-4 rounded-[18px] border border-stone-200 bg-white p-6 sm:p-7">
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
          className="mt-1 rounded-full bg-safran py-3.5 font-bold text-stone-900 hover:bg-safran-light disabled:opacity-60"
        >
          {pending ? 'Création…' : 'Créer mon compte'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-stone-600">
        Déjà un compte ?{' '}
        <Link to={`/connexion?${searchParams}`} className="font-semibold text-brand-700 underline underline-offset-3">
          Se connecter
        </Link>
      </p>
    </div>
  )
}
