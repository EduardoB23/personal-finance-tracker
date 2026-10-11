import { useEffect, useRef, useState, type FormEvent } from 'react'
import { getJson, postJson, PinRequiredError } from '../api.ts'
import { buildBody, canSubmit, emptyForm, payOptions, type Account, type Card, type QuickForm } from '../lib/quickEntry.ts'
import { todayIso, type TransactionType } from '../lib/finance.ts'

type Category = { id: number; name: string; type: 'INCOME' | 'EXPENSE'; archived: boolean }

const TYPE_LABELS: Record<TransactionType, string> = {
  EXPENSE: 'Despesa',
  INCOME: 'Receita',
  TRANSFER: 'Transferência',
}

export function Lancar({ profileId, onPinRequired }: { profileId: number; onPinRequired: () => void }) {
  const [categories, setCategories] = useState<Category[]>([])
  const [accounts, setAccounts] = useState<Account[]>([])
  const [cards, setCards] = useState<Card[]>([])
  const [form, setForm] = useState<QuickForm>(() => emptyForm(todayIso()))
  const [moreOptions, setMoreOptions] = useState(false)
  const [sending, setSending] = useState(false)
  const [feedback, setFeedback] = useState<{ ok: boolean; text: string } | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  // Guarda síncrona: dois toques antes do re-render não podem disparar dois envios (C25)
  const sendingRef = useRef(false)

  useEffect(() => {
    Promise.all([
      getJson<Category[]>('/api/categories'),
      getJson<Account[]>('/api/accounts'),
      getJson<Card[]>('/api/credit-cards'),
    ])
      .then(([loadedCategories, loadedAccounts, loadedCards]) => {
        setCategories(loadedCategories)
        setAccounts(loadedAccounts)
        setCards(loadedCards)
      })
      .catch((error: unknown) => {
        if (error instanceof PinRequiredError) onPinRequired()
        else setLoadError('Não foi possível carregar contas e categorias.')
      })
  }, [onPinRequired])

  const categoryOptions = categories.filter((category) => category.type === form.type && !category.archived)
  const payChoices = payOptions(form.type, accounts, cards)
  const isCard = form.pay?.kind === 'card'

  function changeType(type: TransactionType) {
    setForm((current) => ({ ...current, type, categoryId: null, pay: null, installments: 1, targetAccountId: null }))
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (sendingRef.current || !canSubmit(form)) return
    sendingRef.current = true
    setSending(true)
    setFeedback(null)
    const categoryName = categories.find((category) => category.id === form.categoryId)?.name ?? ''
    try {
      const result = await postJson('/api/transactions', buildBody(form, profileId, categoryName))
      if (result.ok) {
        const text = isCard && form.installments > 1
          ? `Lançamento salvo em ${form.installments} parcelas`
          : 'Lançamento salvo'
        setForm((current) => ({ ...current, amount: '' }))
        setFeedback({ ok: true, text })
      } else {
        setFeedback({ ok: false, text: result.message })
      }
    } catch (error: unknown) {
      if (error instanceof PinRequiredError) onPinRequired()
      else setFeedback({ ok: false, text: 'Não foi possível falar com o servidor.' })
    } finally {
      sendingRef.current = false
      setSending(false)
    }
  }

  if (loadError) return <p role="alert" className="p-4 text-red-700">{loadError}</p>

  const fieldClass = 'rounded-lg border border-stone-400 bg-white px-3 py-3 text-base'

  return (
    <form className="mx-auto grid max-w-md gap-4 px-4 pb-28 pt-6" onSubmit={submit}>
      <h1 className="text-2xl font-semibold">Lançar</h1>

      <div role="radiogroup" aria-label="Tipo" className="grid grid-cols-3 gap-1 rounded-lg bg-stone-200 p-1">
        {(Object.keys(TYPE_LABELS) as TransactionType[]).map((type) => (
          <button
            key={type}
            type="button"
            role="radio"
            aria-checked={form.type === type}
            onClick={() => changeType(type)}
            className={`rounded-md py-2 text-sm font-semibold ${form.type === type ? 'bg-white text-emerald-800 shadow' : 'text-stone-600'}`}
          >
            {TYPE_LABELS[type]}
          </button>
        ))}
      </div>

      <label className="grid gap-1 text-sm font-medium">
        Valor (R$)
        <input
          inputMode="decimal"
          value={form.amount}
          onChange={(event) => setForm((current) => ({ ...current, amount: event.target.value }))}
          className={`${fieldClass} text-2xl`}
        />
      </label>

      {form.type !== 'TRANSFER' && (
        <label className="grid gap-1 text-sm font-medium">
          Categoria
          <select
            value={form.categoryId ?? ''}
            onChange={(event) => setForm((current) => ({ ...current, categoryId: event.target.value ? Number(event.target.value) : null }))}
            className={fieldClass}
          >
            <option value="">Escolha uma categoria</option>
            {categoryOptions.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
          </select>
        </label>
      )}

      <label className="grid gap-1 text-sm font-medium">
        {form.type === 'INCOME' ? 'Recebido em' : 'Pagar com'}
        <select
          value={form.pay ? `${form.pay.kind}:${form.pay.id}` : ''}
          onChange={(event) => {
            const choice = payChoices.find((option) => `${option.kind}:${option.id}` === event.target.value) ?? null
            setForm((current) => ({ ...current, pay: choice, installments: 1 }))
          }}
          className={fieldClass}
        >
          <option value="">Escolha uma conta{form.type === 'EXPENSE' ? ' ou cartão' : ''}</option>
          {payChoices.map((option) => (
            <option key={`${option.kind}:${option.id}`} value={`${option.kind}:${option.id}`}>{option.label}</option>
          ))}
        </select>
      </label>

      <button type="button" onClick={() => setMoreOptions((open) => !open)} aria-expanded={moreOptions} className="justify-self-start text-sm font-semibold text-emerald-800">
        {moreOptions ? 'Menos opções' : 'Mais opções'}
      </button>

      {moreOptions && (
        <div className="grid gap-4 rounded-lg border border-stone-300 bg-white p-3">
          <label className="grid gap-1 text-sm font-medium">
            Descrição
            <input
              value={form.description}
              onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
              className={fieldClass}
            />
          </label>
          <label className="grid gap-1 text-sm font-medium">
            Data
            <input
              type="date"
              value={form.date}
              onChange={(event) => setForm((current) => ({ ...current, date: event.target.value }))}
              className={fieldClass}
            />
          </label>
          {isCard && form.type === 'EXPENSE' && (
            <label className="grid gap-1 text-sm font-medium">
              Parcelas
              <select
                value={form.installments}
                onChange={(event) => setForm((current) => ({ ...current, installments: Number(event.target.value) }))}
                className={fieldClass}
              >
                {Array.from({ length: 24 }, (_, index) => index + 1).map((count) => (
                  <option key={count} value={count}>{count}x</option>
                ))}
              </select>
            </label>
          )}
          <label className="flex items-center gap-2 text-sm font-medium">
            <input
              type="checkbox"
              checked={form.recurring}
              onChange={(event) => setForm((current) => ({ ...current, recurring: event.target.checked }))}
            />
            Repetir todo mês
          </label>
          {form.type === 'TRANSFER' && (
            <label className="grid gap-1 text-sm font-medium">
              Conta de destino
              <select
                value={form.targetAccountId ?? ''}
                onChange={(event) => setForm((current) => ({ ...current, targetAccountId: event.target.value ? Number(event.target.value) : null }))}
                className={fieldClass}
              >
                <option value="">Escolha a conta de destino</option>
                {accounts
                  .filter((account) => form.pay?.kind !== 'account' || account.id !== form.pay.id)
                  .map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
              </select>
            </label>
          )}
        </div>
      )}

      <button
        type="submit"
        disabled={sending || !canSubmit(form)}
        className="rounded-lg bg-emerald-700 py-4 text-lg font-semibold text-white disabled:opacity-50"
      >
        Salvar
      </button>

      {feedback && (
        <p role={feedback.ok ? 'status' : 'alert'} className={feedback.ok ? 'text-emerald-800' : 'text-red-700'}>
          {feedback.text}
        </p>
      )}
    </form>
  )
}
