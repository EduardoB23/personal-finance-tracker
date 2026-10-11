import { useEffect, useState, type FormEvent } from 'react'
import { getJson, putJson, PinRequiredError } from '../api.ts'
import { currentMonth, forProfile, monthLabel, shiftMonth, summarize, type Transaction } from '../lib/finance.ts'
import { payOptions, type Account, type Card, type QuickForm } from '../lib/quickEntry.ts'
import { buildEditBody, formFromTransaction, isEditable } from '../lib/transactionEdit.ts'
import type { Profile } from './QuemSouEu.tsx'

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
type Category = { id: number; name: string; type: 'INCOME' | 'EXPENSE'; archived: boolean }

export function Extrato({ profiles, onPinRequired }: { profiles: Profile[]; onPinRequired: () => void }) {
  const [month, setMonth] = useState(currentMonth())
  const [personId, setPersonId] = useState<number | null>(null)
  const [transactions, setTransactions] = useState<Transaction[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)
  const [accounts, setAccounts] = useState<Account[]>([])
  const [cards, setCards] = useState<Card[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState<QuickForm | null>(null)
  const [feedback, setFeedback] = useState<{ ok: boolean; text: string } | null>(null)

  useEffect(() => {
    Promise.all([getJson<Account[]>('/api/accounts'), getJson<Card[]>('/api/credit-cards'), getJson<Category[]>('/api/categories')])
      .then(([loadedAccounts, loadedCards, loadedCategories]) => {
        setAccounts(loadedAccounts)
        setCards(loadedCards)
        setCategories(loadedCategories)
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof PinRequiredError) onPinRequired()
      })
  }, [onPinRequired])

  useEffect(() => {
    let active = true
    getJson<Transaction[]>(`/api/transactions?month=${month}`)
      .then((list) => {
        if (active) {
          setTransactions(list)
          setError(null)
        }
      })
      .catch((requestError: unknown) => {
        if (!active) return
        if (requestError instanceof PinRequiredError) onPinRequired()
        else setError('Não foi possível carregar o extrato.')
      })
    return () => {
      active = false
    }
  }, [month, reload, onPinRequired])

  if (error) return <p role="alert" className="p-4 text-red-700">{error}</p>
  if (transactions === null) return <p className="p-4 text-stone-600">Carregando o extrato...</p>

  const visible = [...forProfile(transactions, personId)].sort((a, b) =>
    b.date.localeCompare(a.date) || b.id - a.id)
  const totals = summarize(visible)
  const nameOf = (profileId: number) => profiles.find((profile) => profile.id === profileId)?.name ?? ''

  function startEdit(transaction: Transaction) {
    setEditingId(transaction.id)
    setEditForm(formFromTransaction(transaction))
    setFeedback(null)
  }

  async function saveEdit(event: FormEvent<HTMLFormElement>, transaction: Transaction) {
    event.preventDefault()
    if (!editForm) return
    const categoryName = categories.find((category) => category.id === editForm.categoryId)?.name ?? ''
    try {
      const result = await putJson(`/api/transactions/${transaction.id}`, buildEditBody(editForm, transaction, categoryName))
      if (result.ok) {
        setEditingId(null)
        setEditForm(null)
        setFeedback({ ok: true, text: 'Lançamento atualizado' })
        setReload((value) => value + 1)
      } else {
        setFeedback({ ok: false, text: result.message })
      }
    } catch (requestError: unknown) {
      if (requestError instanceof PinRequiredError) onPinRequired()
      else setFeedback({ ok: false, text: 'Não foi possível falar com o servidor.' })
    }
  }

  const fieldClass = 'rounded-lg border border-stone-400 bg-white px-3 py-2 text-base'

  return (
    <section className="mx-auto grid max-w-md gap-4 px-4 pb-28 pt-6">
      <div className="flex items-center justify-between">
        <button type="button" aria-label="Mês anterior" onClick={() => setMonth((current) => shiftMonth(current, -1))} className="rounded-lg px-3 py-2 text-xl">‹</button>
        <h1 className="text-xl font-semibold">{monthLabel(month)}</h1>
        <button type="button" aria-label="Próximo mês" onClick={() => setMonth((current) => shiftMonth(current, 1))} className="rounded-lg px-3 py-2 text-xl">›</button>
      </div>

      <div className="flex gap-2 overflow-x-auto" role="group" aria-label="Filtrar por pessoa">
        <button type="button" aria-pressed={personId === null} onClick={() => setPersonId(null)} className={chipClass(personId === null)}>Todos</button>
        {profiles.map((profile) => (
          <button key={profile.id} type="button" aria-pressed={personId === profile.id} onClick={() => setPersonId(profile.id)} className={chipClass(personId === profile.id)}>
            {profile.name}
          </button>
        ))}
      </div>

      <dl className="grid grid-cols-3 gap-2 text-center text-sm">
        <div className="rounded-lg bg-white p-2"><dt className="text-stone-500">Entradas</dt><dd className="font-semibold text-emerald-800">{currency.format(totals.income)}</dd></div>
        <div className="rounded-lg bg-white p-2"><dt className="text-stone-500">Saídas</dt><dd className="font-semibold text-red-700">{currency.format(totals.expense)}</dd></div>
        <div className="rounded-lg bg-white p-2"><dt className="text-stone-500">Saldo</dt><dd className="font-semibold">{currency.format(totals.balance)}</dd></div>
      </dl>

      {feedback && <p role={feedback.ok ? 'status' : 'alert'} className={feedback.ok ? 'text-sm text-emerald-800' : 'text-sm text-red-700'}>{feedback.text}</p>}

      {visible.length === 0 ? (
        <p className="text-stone-600">Nenhum lançamento neste mês</p>
      ) : (
        <ul className="grid gap-2">
          {visible.map((transaction) => {
            const editing = editingId === transaction.id && editForm !== null
            const categoriesForType = categories.filter((category) => category.type === transaction.type && !category.archived)
            return (
              <li key={transaction.id} className="grid gap-2 rounded-lg bg-white p-3">
                {editing ? (
                  <form noValidate className="grid gap-2" onSubmit={(event) => void saveEdit(event, transaction)}>
                    <p className="text-sm font-semibold">Editar lançamento</p>
                    <label className="grid gap-1 text-sm font-medium">
                      Descrição
                      <input value={editForm.description} onChange={(event) => setEditForm({ ...editForm, description: event.target.value })} className={fieldClass} />
                    </label>
                    <label className="grid gap-1 text-sm font-medium">
                      Valor (R$)
                      <input inputMode="decimal" value={editForm.amount} onChange={(event) => setEditForm({ ...editForm, amount: event.target.value })} className={fieldClass} />
                    </label>
                    {transaction.type !== 'TRANSFER' && (
                      <label className="grid gap-1 text-sm font-medium">
                        Categoria
                        <select value={editForm.categoryId ?? ''} onChange={(event) => setEditForm({ ...editForm, categoryId: event.target.value ? Number(event.target.value) : null })} className={fieldClass}>
                          {categoriesForType.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
                        </select>
                      </label>
                    )}
                    <label className="grid gap-1 text-sm font-medium">
                      {transaction.type === 'INCOME' ? 'Recebido em' : 'Pagar com'}
                      <select
                        value={editForm.pay ? `${editForm.pay.kind}:${editForm.pay.id}` : ''}
                        onChange={(event) => {
                          const choice = payOptions(transaction.type, accounts, cards).find((option) => `${option.kind}:${option.id}` === event.target.value) ?? null
                          setEditForm({ ...editForm, pay: choice })
                        }}
                        className={fieldClass}
                      >
                        {payOptions(transaction.type, accounts, cards).map((option) => (
                          <option key={`${option.kind}:${option.id}`} value={`${option.kind}:${option.id}`}>{option.label}</option>
                        ))}
                      </select>
                    </label>
                    <label className="grid gap-1 text-sm font-medium">
                      Data
                      <input type="date" value={editForm.date} onChange={(event) => setEditForm({ ...editForm, date: event.target.value })} className={fieldClass} />
                    </label>
                    <div className="flex gap-2">
                      <button type="submit" className="flex-1 rounded-lg bg-emerald-700 py-2 font-semibold text-white">Salvar</button>
                      <button type="button" onClick={() => { setEditingId(null); setEditForm(null) }} className="rounded-lg border border-stone-400 px-3 py-2 text-sm">Cancelar</button>
                    </div>
                  </form>
                ) : (
                  <div className="flex items-center justify-between gap-3">
                    <span className="min-w-0">
                      <span className="block truncate font-medium">{transaction.description}</span>
                      <span className="text-xs text-stone-500">
                        {transaction.date.split('-').reverse().join('/')} · {nameOf(transaction.profileId)}
                        {transaction.type === 'TRANSFER' ? ' · Transferência' : ''}
                      </span>
                    </span>
                    <span className="flex shrink-0 items-center gap-2">
                      <span className={`font-semibold ${transaction.type === 'INCOME' ? 'text-emerald-800' : transaction.type === 'EXPENSE' ? 'text-red-700' : 'text-stone-600'}`}>
                        {currency.format(transaction.amount)}
                      </span>
                      {isEditable(transaction) ? (
                        <button type="button" onClick={() => startEdit(transaction)} className="rounded-lg border border-stone-400 px-2 py-1 text-xs">Editar</button>
                      ) : null}
                    </span>
                  </div>
                )}
                {!editing && !isEditable(transaction) && (
                  <p className="text-xs text-stone-500">Parcelas e recorrências não podem ser editadas por aqui.</p>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}

function chipClass(active: boolean): string {
  return `shrink-0 rounded-full px-3 py-1 text-sm ${active ? 'bg-emerald-700 text-white' : 'bg-white text-stone-700'}`
}
