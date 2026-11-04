import { type FormEvent, useState } from 'react'
import type { StoreSettings } from '../../api/types.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { FormField } from '../../components/FormField.tsx'
import { btnPrimary, PageHeader, Panel } from '../../components/admin/ui.tsx'
import { useUpdateStoreSettings } from '../../hooks/useAdmin.ts'
import { useStoreSettings } from '../../hooks/useShop.ts'
import { fieldErrors } from '../../lib/forms.ts'
import { useToast } from '../../lib/toast.ts'

// Interrupteur accessible : une vraie case à cocher (role="switch"), stylée comme la maquette
function Toggle({ name, label, hint, checked, onChange }: { name: string; label: string; hint: string; checked: boolean; onChange: (value: boolean) => void }) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-stone-200 py-3.5">
      <label htmlFor={name}>
        <b className="block font-semibold">{label}</b>
        <span className="text-sm text-stone-600">{hint}</span>
      </label>
      <input
        id={name}
        name={name}
        type="checkbox"
        role="switch"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="relative h-6.5 w-11 shrink-0 cursor-pointer appearance-none rounded-full bg-stone-200 transition after:absolute after:top-[3px] after:left-[3px] after:size-5 after:rounded-full after:bg-white after:transition checked:bg-kola checked:after:translate-x-[18px]"
      />
    </div>
  )
}

// Les réglages ont un vrai effet : le serveur les applique à chaque commande (moyens de paiement, plafond, frais, zones)
export function AdminSettingsPage() {
  const settings = useStoreSettings()
  const save = useUpdateStoreSettings()
  const toast = useToast()
  const errors = fieldErrors(save.error)

  if (settings.isError) {
    return <ErrorMessage error={settings.error} />
  }
  if (!settings.data) {
    return <p className="text-stone-600">Chargement…</p>
  }
  return (
    <SettingsForm
      key={JSON.stringify(settings.data)}
      initial={settings.data}
      saving={save.isPending}
      errors={errors}
      error={save.isError && Object.keys(errors).length === 0 ? save.error : null}
      onSave={(values) => save.mutate(values, { onSuccess: () => toast('Paramètres enregistrés') })}
    />
  )
}

type Settings = StoreSettings

function SettingsForm({ initial, saving, errors, error, onSave }: { initial: Settings; saving: boolean; errors: Record<string, string>; error: unknown; onSave: (values: Settings) => void }) {
  const [online, setOnline] = useState(initial.onlinePaymentEnabled)
  const [cod, setCod] = useState(initial.codEnabled)

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    // Espèces désactivées : leurs champs ne sont pas affichés, on garde les valeurs précédentes
    const max = cod ? String(form.get('codMaxAmount') ?? '').trim() : String(initial.codMaxAmount ?? '')
    onSave({
      storeName: String(form.get('storeName')).trim(),
      supportEmail: String(form.get('supportEmail')).trim(),
      onlinePaymentEnabled: online,
      codEnabled: cod,
      codMaxAmount: max ? Number(max) : null,
      codFee: cod ? Number(form.get('codFee') || 0) : initial.codFee,
      codZones: (cod ? String(form.get('codZones') ?? '') : initial.codZones.join(','))
        .split(',')
        .map((zone) => zone.trim())
        .filter(Boolean),
    })
  }

  return (
    <>
      <PageHeader title="Paramètres" description="Configuration générale de la boutique." />
      <form onSubmit={onSubmit}>
        {error !== null && <ErrorMessage error={error} />}
        <div className="grid items-start gap-4 xl:grid-cols-2">
          <Panel title="Informations de la boutique">
            <div className="flex flex-col gap-3.5">
              <FormField name="storeName" label="Nom de la boutique" required maxLength={100} defaultValue={initial.storeName} error={errors.storeName} />
              <FormField name="supportEmail" label="E-mail du service client" type="email" required maxLength={255} defaultValue={initial.supportEmail} hint="Affiché en bas de chaque page de la boutique." error={errors.supportEmail} />
              <FormField name="currency" label="Devise" value="FCFA (XOF)" readOnly disabled />
            </div>
          </Panel>

          <Panel title="Moyens de paiement">
            <Toggle name="online" label="Paiement en ligne" hint="Wave, Orange Money et carte bancaire (via PayDunya)" checked={online} onChange={setOnline} />
            <Toggle name="cod" label="Espèces à la livraison" hint="Le client paie le livreur à la réception du colis" checked={cod} onChange={setCod} />
            {cod && (
              <div className="mt-4 flex flex-col gap-3.5">
                <div className="grid gap-3 sm:grid-cols-2">
                  <FormField name="codMaxAmount" label="Plafond par commande (FCFA)" type="number" min={1} step={1} defaultValue={initial.codMaxAmount ?? ''} hint="Vide = sans plafond" error={errors.codMaxAmount} />
                  <FormField name="codFee" label="Frais de service (FCFA)" type="number" min={0} step={1} defaultValue={initial.codFee} error={errors.codFee} />
                </div>
                <FormField name="codZones" label="Villes desservies" maxLength={500} defaultValue={initial.codZones.join(', ')} placeholder="Dakar, Pikine, Guédiawaye, Rufisque" hint="Séparées par des virgules. Vide = partout." />
                <p className="text-[13px] text-stone-600">Au-delà du plafond, le client doit payer en ligne. Les frais s’ajoutent au total de la commande.</p>
              </div>
            )}
            {!online && !cod && <p className="mt-3 text-sm font-medium text-bissap">Attention : sans moyen de paiement, plus personne ne peut commander.</p>}
          </Panel>
        </div>
        <div className="mt-4 flex justify-end">
          <button type="submit" disabled={saving} className={btnPrimary}>
            {saving ? 'Enregistrement…' : 'Enregistrer les paramètres'}
          </button>
        </div>
      </form>
    </>
  )
}
