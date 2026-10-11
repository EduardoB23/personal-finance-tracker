import { useState, type FormEvent } from 'react'
import { postJson, PinRequiredError } from '../api.ts'

const ACCOUNT_TYPES = [
  { value: 'CHECKING', label: 'Conta corrente' },
  { value: 'SAVINGS', label: 'Poupança' },
  { value: 'WALLET', label: 'Carteira' },
  { value: 'INVESTMENT', label: 'Investimento' },
]

type Feedback = { ok: boolean; text: string }

export function Cadastro({ onBack, onSaved, onPinRequired }: {
  onBack: () => void
  onSaved: () => void
  onPinRequired: () => void
}) {
  const [feedback, setFeedback] = useState<Record<string, Feedback | null>>({})

  async function save(section: string, url: string, body: unknown, event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    try {
      const result = await postJson(url, body)
      if (result.ok) {
        form.reset()
        setFeedback((current) => ({ ...current, [section]: { ok: true, text: 'Cadastrado' } }))
        onSaved()
      } else {
        setFeedback((current) => ({ ...current, [section]: { ok: false, text: result.message } }))
      }
    } catch (error: unknown) {
      if (error instanceof PinRequiredError) onPinRequired()
      else setFeedback((current) => ({ ...current, [section]: { ok: false, text: 'Não foi possível falar com o servidor.' } }))
    }
  }

  const input = 'rounded-lg border border-stone-400 bg-white px-3 py-3 text-base'

  return (
    <div className="mx-auto grid max-w-md gap-6 px-4 pb-28 pt-6">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Cadastros</h1>
        <button type="button" onClick={onBack} className="rounded-lg border border-stone-400 bg-white px-3 py-2 text-sm">Voltar</button>
      </header>

      <form noValidate className="grid gap-3 rounded-xl bg-white p-4" onSubmit={(event) => {
        const data = new FormData(event.currentTarget)
        void save('profile', '/api/profiles', { name: String(data.get('name') ?? '') }, event)
      }}>
        <h2 className="font-semibold">Perfil</h2>
        <input name="name" aria-label="Nome do perfil" placeholder="Nome" className={input} />
        <button type="submit" className="rounded-lg bg-emerald-700 py-3 font-semibold text-white">Cadastrar perfil</button>
        <Message value={feedback.profile} />
      </form>

      <form noValidate className="grid gap-3 rounded-xl bg-white p-4" onSubmit={(event) => {
        const data = new FormData(event.currentTarget)
        void save('account', '/api/accounts', {
          name: String(data.get('name') ?? ''),
          type: String(data.get('type')),
          balance: Number(String(data.get('balance') ?? '0').replace(',', '.')),
        }, event)
      }}>
        <h2 className="font-semibold">Conta</h2>
        <input name="name" aria-label="Nome da conta" placeholder="Nome" className={input} />
        <select name="type" aria-label="Tipo da conta" className={input} defaultValue="CHECKING">
          {ACCOUNT_TYPES.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}
        </select>
        <input name="balance" aria-label="Saldo inicial" inputMode="decimal" placeholder="Saldo inicial (R$)" className={input} />
        <button type="submit" className="rounded-lg bg-emerald-700 py-3 font-semibold text-white">Cadastrar conta</button>
        <Message value={feedback.account} />
      </form>

      <form noValidate className="grid gap-3 rounded-xl bg-white p-4" onSubmit={(event) => {
        const data = new FormData(event.currentTarget)
        const limit = Number(String(data.get('limit') ?? '0').replace(',', '.'))
        void save('card', '/api/credit-cards', {
          name: String(data.get('name') ?? ''),
          limitAmount: limit,
          availableLimit: limit,
          closingDay: Number(data.get('closingDay')),
          dueDay: Number(data.get('dueDay')),
        }, event)
      }}>
        <h2 className="font-semibold">Cartão</h2>
        <input name="name" aria-label="Nome do cartão" placeholder="Nome" className={input} />
        <input name="limit" aria-label="Limite" inputMode="decimal" placeholder="Limite (R$)" className={input} />
        <input name="closingDay" aria-label="Dia de fechamento" type="number" min={1} max={31} placeholder="Dia de fechamento" className={input} />
        <input name="dueDay" aria-label="Dia de vencimento" type="number" min={1} max={31} placeholder="Dia de vencimento" className={input} />
        <button type="submit" className="rounded-lg bg-emerald-700 py-3 font-semibold text-white">Cadastrar cartão</button>
        <Message value={feedback.card} />
      </form>
    </div>
  )
}

function Message({ value }: { value: Feedback | null | undefined }) {
  if (!value) return null
  return <p role={value.ok ? 'status' : 'alert'} className={value.ok ? 'text-sm text-emerald-800' : 'text-sm text-red-700'}>{value.text}</p>
}
