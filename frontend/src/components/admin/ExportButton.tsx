import { useState } from 'react'
import { downloadExport, type ExportName } from '../../api/admin.ts'
import { useToast } from '../../lib/toast.ts'
import { Icon } from '../Icon.tsx'
import { btnGhost } from './ui.tsx'

// Télécharge un export CSV (ouvrable dans Excel). Le fichier est demandé avec le jeton, puis enregistré
export function ExportButton({ name, label = 'Exporter' }: { name: ExportName; label?: string }) {
  const [pending, setPending] = useState(false)
  const toast = useToast()

  async function onClick() {
    setPending(true)
    try {
      await downloadExport(name)
      toast('Fichier téléchargé (à ouvrir avec Excel, LibreOffice ou Google Sheets)')
    } catch (error) {
      toast(error instanceof Error ? error.message : 'Export impossible')
    } finally {
      setPending(false)
    }
  }

  return (
    <button type="button" onClick={onClick} disabled={pending} className={btnGhost}>
      <Icon name="download" className="size-4" />
      {pending ? 'Préparation…' : label}
    </button>
  )
}
