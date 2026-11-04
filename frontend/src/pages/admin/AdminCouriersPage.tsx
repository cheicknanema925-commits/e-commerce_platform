import { type FormEvent, useState } from 'react'
import type { Courier } from '../../api/types.ts'
import { ErrorMessage } from '../../components/ErrorMessage.tsx'
import { FormField } from '../../components/FormField.tsx'
import { btnMini, btnPrimary, PageHeader, Panel, TableCard, td, th } from '../../components/admin/ui.tsx'
import { useCouriers, useCreateCourier, useUpdateCourier } from '../../hooks/useAdmin.ts'
import { fieldErrors } from '../../lib/forms.ts'
import { useToast } from '../../lib/toast.ts'

// Livreurs : on ne les supprime pas (l'historique des livraisons doit rester complet), on les désactive
export function AdminCouriersPage() {
  const couriers = useCouriers()
  const create = useCreateCourier()
  const update = useUpdateCourier()
  const toast = useToast()
  const [editing, setEditing] = useState<number | null>(null)
  const createErrors = fieldErrors(create.error)

  function onCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const data = new FormData(form)
    create.mutate(
      { name: String(data.get('name')).trim(), phone: String(data.get('phone')).trim(), active: true },
      {
        onSuccess: (courier) => {
          toast(`Livreur ajouté : ${courier.name}`)
          form.reset()
        },
      },
    )
  }

  function save(courier: Courier, changes: Partial<Courier>) {
    update.mutate(
      { id: courier.id, courier: { name: courier.name, phone: courier.phone, active: courier.active, ...changes } },
      { onSuccess: () => toast('Livreur mis à jour'), onError: (error) => toast(error.message) },
    )
  }

  return (
    <>
      <PageHeader title="Livreurs" description="Ajoutez vos livreurs, puis confiez-leur les commandes depuis la page Commandes ou Paiements." />

      <div className="grid items-start gap-4 xl:grid-cols-[1.55fr_1fr]">
        <div className="min-w-0">
          {couriers.isError && <ErrorMessage error={couriers.error} />}
          <TableCard title="Équipe de livraison" count={couriers.data?.length}>
            <table className="w-full text-sm">
              <thead>
                <tr>
                  <th scope="col" className={th}>Livreur</th>
                  <th scope="col" className={th}>Téléphone</th>
                  <th scope="col" className={`${th} text-right`}>Livraisons en cours</th>
                  <th scope="col" className={th}>Statut</th>
                  <th scope="col" className={th}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {couriers.data?.map((courier) =>
                  editing === courier.id ? (
                    <CourierEditRow
                      key={courier.id}
                      courier={courier}
                      onCancel={() => setEditing(null)}
                      onSave={(name, phone) => {
                        save(courier, { name, phone })
                        setEditing(null)
                      }}
                    />
                  ) : (
                    <tr key={courier.id} className="hover:bg-stone-50">
                      <td className={`${td} font-semibold`}>{courier.name}</td>
                      <td className={td}>{courier.phone}</td>
                      <td className={`${td} text-right tabular-nums`}>{courier.ongoingDeliveries}</td>
                      <td className={td}>
                        <span className={`rounded-full px-2.5 py-0.5 text-[13px] font-semibold ${courier.active ? 'bg-kola-soft text-kola' : 'bg-stone-200 text-stone-600'}`}>
                          {courier.active ? 'Actif' : 'Désactivé'}
                        </span>
                      </td>
                      <td className={td}>
                        <div className="flex gap-1.5">
                          <button type="button" onClick={() => setEditing(courier.id)} className={btnMini}>
                            Modifier<span className="sr-only"> {courier.name}</span>
                          </button>
                          <button type="button" disabled={update.isPending} onClick={() => save(courier, { active: !courier.active })} className={btnMini}>
                            {courier.active ? 'Désactiver' : 'Réactiver'}
                            <span className="sr-only"> {courier.name}</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ),
                )}
              </tbody>
            </table>
            {couriers.data?.length === 0 && <p className="p-9 text-center text-stone-600">Aucun livreur pour l'instant : ajoutez le premier.</p>}
          </TableCard>
        </div>

        <div className="mt-4.5">
          <Panel title="Ajouter un livreur">
            <form onSubmit={onCreate} className="flex flex-col gap-3.5">
              {create.isError && Object.keys(createErrors).length === 0 && <ErrorMessage error={create.error} />}
              <FormField name="name" label="Nom *" required maxLength={100} placeholder="Ex. Abdou K." error={createErrors.name} />
              <FormField name="phone" label="Téléphone *" type="tel" required pattern="^\+?[0-9 ]{9,20}$" placeholder="+221 77 123 45 67" error={createErrors.phone} />
              <button type="submit" disabled={create.isPending} className={`${btnPrimary} justify-center`}>
                {create.isPending ? 'Ajout…' : 'Ajouter le livreur'}
              </button>
            </form>
          </Panel>
        </div>
      </div>
    </>
  )
}

function CourierEditRow({ courier, onSave, onCancel }: { courier: Courier; onSave: (name: string, phone: string) => void; onCancel: () => void }) {
  const [name, setName] = useState(courier.name)
  const [phone, setPhone] = useState(courier.phone)
  const input = 'w-full rounded-lg border border-stone-200 px-2.5 py-1.5'
  return (
    <tr className="bg-brand-50">
      <td className={td}>
        <label className="sr-only" htmlFor={`nom-${courier.id}`}>
          Nom
        </label>
        <input id={`nom-${courier.id}`} value={name} maxLength={100} onChange={(event) => setName(event.target.value)} className={input} />
      </td>
      <td className={td}>
        <label className="sr-only" htmlFor={`tel-${courier.id}`}>
          Téléphone
        </label>
        <input id={`tel-${courier.id}`} value={phone} maxLength={20} onChange={(event) => setPhone(event.target.value)} className={input} />
      </td>
      <td className={td} />
      <td className={td} />
      <td className={td}>
        <div className="flex gap-1.5">
          <button type="button" disabled={!name.trim() || !phone.trim()} onClick={() => onSave(name.trim(), phone.trim())} className={btnMini}>
            Enregistrer
          </button>
          <button type="button" onClick={onCancel} className={btnMini}>
            Annuler
          </button>
        </div>
      </td>
    </tr>
  )
}
