import { useState, type FormEvent } from 'react'

export function PinScreen({ onUnlocked }: { onUnlocked: () => void }) {
  const [pin, setPin] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const [sending, setSending] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSending(true)
    setMessage(null)
    try {
      const response = await fetch('/api/auth/pin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin }),
      })
      if (response.status === 204) {
        onUnlocked()
        return
      }
      const body = (await response.json().catch(() => ({}))) as { message?: string }
      setMessage(body.message ?? 'Não foi possível entrar.')
    } catch {
      setMessage('Não foi possível falar com o servidor.')
    } finally {
      setSending(false)
    }
  }

  return (
    <main className="mx-auto flex min-h-dvh max-w-md items-center px-4">
      <form className="grid w-full gap-4" onSubmit={submit}>
        <h1 className="text-2xl font-semibold">Entrar no painel</h1>
        <label className="grid gap-1 text-sm font-medium">
          PIN familiar
          <input
            type="password"
            inputMode="numeric"
            autoComplete="current-password"
            autoFocus
            value={pin}
            onChange={(event) => setPin(event.target.value)}
            className="rounded-lg border border-stone-400 bg-white px-3 py-3 text-2xl tracking-[0.3em]"
          />
        </label>
        <button type="submit" disabled={sending} className="rounded-lg bg-emerald-700 py-3 font-semibold text-white disabled:opacity-60">
          Entrar
        </button>
        {message && <p role="alert" className="text-sm text-red-700">{message}</p>}
      </form>
    </main>
  )
}
