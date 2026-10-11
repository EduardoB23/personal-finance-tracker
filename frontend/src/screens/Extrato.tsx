import { useEffect, useState } from 'react'
import { getJson, PinRequiredError } from '../api.ts'
import { currentMonth, forProfile, monthLabel, shiftMonth, summarize, type Transaction } from '../lib/finance.ts'
import type { Profile } from './QuemSouEu.tsx'

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function Extrato({ profiles, onPinRequired }: { profiles: Profile[]; onPinRequired: () => void }) {
  const [month, setMonth] = useState(currentMonth())
  const [personId, setPersonId] = useState<number | null>(null)
  const [transactions, setTransactions] = useState<Transaction[] | null>(null)
  const [error, setError] = useState<string | null>(null)

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
  }, [month, onPinRequired])

  if (error) return <p role="alert" className="p-4 text-red-700">{error}</p>
  if (transactions === null) return <p className="p-4 text-stone-600">Carregando o extrato...</p>

  const visible = [...forProfile(transactions, personId)].sort((a, b) =>
    b.date.localeCompare(a.date) || b.id - a.id)
  const totals = summarize(visible)
  const nameOf = (profileId: number) => profiles.find((profile) => profile.id === profileId)?.name ?? ''

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

      {visible.length === 0 ? (
        <p className="text-stone-600">Nenhum lançamento neste mês</p>
      ) : (
        <ul className="grid gap-2">
          {visible.map((transaction) => (
            <li key={transaction.id} className="flex items-center justify-between gap-3 rounded-lg bg-white p-3">
              <span className="min-w-0">
                <span className="block truncate font-medium">{transaction.description}</span>
                <span className="text-xs text-stone-500">
                  {transaction.date.split('-').reverse().join('/')} · {nameOf(transaction.profileId)}
                  {transaction.type === 'TRANSFER' ? ' · Transferência' : ''}
                </span>
              </span>
              <span className={`shrink-0 font-semibold ${transaction.type === 'INCOME' ? 'text-emerald-800' : transaction.type === 'EXPENSE' ? 'text-red-700' : 'text-stone-600'}`}>
                {currency.format(transaction.amount)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function chipClass(active: boolean): string {
  return `shrink-0 rounded-full px-3 py-1 text-sm ${active ? 'bg-emerald-700 text-white' : 'bg-white text-stone-700'}`
}
