import type { InputHTMLAttributes } from 'react'

type FormFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  name: string
  label: string
  hint?: string
  error?: string
}

export const inputClass =
  'w-full rounded-lg border bg-white px-3 py-2.5 text-[15px] outline-none transition focus:border-brand-700 focus:ring-3 focus:ring-brand-700/15'

// Libellé, aide et erreur reliés au champ (aria-describedby) : un lecteur d'écran lit tout au bon moment
export function FormField({ name, label, hint, error, ...input }: FormFieldProps) {
  const describedBy = [hint && `${name}-aide`, error && `${name}-erreur`].filter(Boolean).join(' ') || undefined
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={name} className="text-sm font-semibold text-stone-900">
        {label}
      </label>
      <input
        id={name}
        name={name}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={`${inputClass} ${error ? 'border-bissap' : 'border-stone-200'}`}
        {...input}
      />
      {hint && (
        <p id={`${name}-aide`} className="text-xs text-stone-600">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${name}-erreur`} className="text-sm font-medium text-bissap">
          {error}
        </p>
      )}
    </div>
  )
}
