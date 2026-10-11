import { useState, type ChangeEvent } from 'react'
import { PinRequiredError } from '../api.ts'

type Feedback = { ok: boolean; text: string }

export function Configuracoes({ onBack, onImported, onPinRequired }: {
  onBack: () => void
  onImported: () => void
  onPinRequired: () => void
}) {
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const [pendingFile, setPendingFile] = useState<File | null>(null)
  const [sending, setSending] = useState(false)

  async function exportBackup(format: 'json' | 'csv') {
    setFeedback(null)
    const response = await fetch(`/api/backup/export?format=${format}`)
    if (response.status === 401) return onPinRequired()
    if (!response.ok) return setFeedback({ ok: false, text: 'Não foi possível exportar o backup.' })
    const blob = await response.blob()
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `finance-backup.${format}`
    link.click()
    URL.revokeObjectURL(link.href)
    setFeedback({ ok: true, text: 'Backup exportado' })
  }

  function chooseFile(event: ChangeEvent<HTMLInputElement>) {
    setFeedback(null)
    setPendingFile(event.target.files?.[0] ?? null)
  }

  async function importBackup() {
    if (!pendingFile || sending) return
    setSending(true)
    setFeedback(null)
    try {
      const body = new FormData()
      body.append('file', pendingFile)
      const response = await fetch('/api/backup/import', { method: 'POST', body })
      if (response.status === 401) throw new PinRequiredError()
      const data = (await response.json().catch(() => ({}))) as { message?: string }
      if (response.ok) {
        setPendingFile(null)
        setFeedback({ ok: true, text: data.message ?? 'Backup importado com sucesso' })
        onImported()
      } else {
        setFeedback({ ok: false, text: data.message ?? 'Não foi possível importar o backup.' })
      }
    } catch (error: unknown) {
      if (error instanceof PinRequiredError) onPinRequired()
      else setFeedback({ ok: false, text: 'Não foi possível falar com o servidor.' })
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="mx-auto grid max-w-md gap-6 px-4 pb-28 pt-6">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Configurações</h1>
        <button type="button" onClick={onBack} className="rounded-lg border border-stone-400 bg-white px-3 py-2 text-sm">Voltar</button>
      </header>

      <section aria-label="Exportar" className="grid gap-3 rounded-xl bg-white p-4">
        <h2 className="font-semibold">Exportar dados</h2>
        <button type="button" onClick={() => void exportBackup('json')} className="rounded-lg bg-emerald-700 py-3 font-semibold text-white">Exportar JSON</button>
        <button type="button" onClick={() => void exportBackup('csv')} className="rounded-lg border border-stone-400 py-3 font-semibold">Exportar CSV</button>
      </section>

      <section aria-label="Importar" className="grid gap-3 rounded-xl bg-white p-4">
        <h2 className="font-semibold">Importar dados</h2>
        <input type="file" accept=".json,.csv" aria-label="Arquivo de backup" onChange={chooseFile} className="text-sm" />
        {pendingFile && (
          <div className="grid gap-2 rounded-lg border border-red-300 bg-red-50 p-3">
            <p className="text-sm">Importar substitui todos os dados atuais. Continuar?</p>
            <div className="flex gap-2">
              <button type="button" onClick={() => setPendingFile(null)} className="rounded-lg border border-stone-400 px-3 py-2 text-sm">Cancelar</button>
              <button type="button" disabled={sending} onClick={() => void importBackup()} className="rounded-lg bg-red-700 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50">Importar</button>
            </div>
          </div>
        )}
      </section>

      {feedback && <p role={feedback.ok ? 'status' : 'alert'} className={feedback.ok ? 'text-emerald-800' : 'text-red-700'}>{feedback.text}</p>}
    </div>
  )
}
