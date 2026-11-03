import type { InputHTMLAttributes } from 'react'

type FormFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  name: string
  label: string
  hint?: string
  error?: string
}

// Libellé, aide et erreur reliés au champ (aria-describedby) : un lecteur d'écran lit tout au bon moment
export function FormField({ name, label, hint, error, ...input }: FormFieldProps) {
  const describedBy = [hint && `${name}-aide`, error && `${name}-erreur`].filter(Boolean).join(' ') || undefined
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={name} className="text-sm font-medium text-stone-700">
        {label}
      </label>
      <input
        id={name}
        name={name}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={`rounded-md border px-3 py-2 ${error ? 'border-red-600' : 'border-stone-300'}`}
        {...input}
      />
      {hint && (
        <p id={`${name}-aide`} className="text-xs text-stone-500">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${name}-erreur`} className="text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  )
}
