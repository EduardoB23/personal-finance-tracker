import { useEffect, useRef, useState, type FormEvent } from 'react'
import { getJson, postJson, PinRequiredError } from '../api.ts'
import { canCreateBudget } from '../lib/budgetsGoals.ts'
import { currentMonth, monthLabel, shiftMonth } from '../lib/finance.ts'

type Category = { id: number; name: string; type: 'INCOME' | 'EXPENSE'; archived: boolean }
type BudgetSummary = { id: number; category: string; limitAmount: number; consumedAmount: number; consumedPercentage: number }

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function Orcamentos({ onBack, onPinRequired }: { onBack: () => void; onPinRequired: () => void }) {
  const [month, setMonth] = useState(currentMonth())
  const [budgets, setBudgets] = useState<BudgetSummary[] | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [limit, setLimit] = useState('')
  const [feedback, setFeedback] = useState<{ ok: boolean; text: string } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)
  const sendingRef = useRef(false)

  useEffect(() => {
    getJson<Category[]>('/api/categories?type=EXPENSE')
      .then(setCategories)
      .catch((requestError: unknown) => {
        if (requestError instanceof PinRequiredError) onPinRequired()
        else setError('Não foi possível carregar as categorias.')
      })
  }, [onPinRequired])

  useEffect(() => {
    let active = true
    getJson<BudgetSummary[]>(`/api/budgets/summary?month=${month}`)
      .then((list) => { if (active) { setBudgets(list); setError(null) } })
      .catch((requestError: unknown) => {
        if (!active) return
        if (requestError instanceof PinRequiredError) onPinRequired()
        else setError('Não foi possível carregar os orçamentos.')
      })
    return () => { active = false }
  }, [month, reload, onPinRequired])

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (sendingRef.current || !canCreateBudget(categoryId, limit)) return
    sendingRef.current = true
    try {
      const result = await postJson('/api/budgets', {
        categoryId,
        limitAmount: Number(limit.replace(',', '.')),
        month,
      })
      if (result.ok) {
        setLimit('')
        setFeedback({ ok: true, text: 'Orçamento criado' })
        setReload((value) => value + 1)
      } else {
        setFeedback({ ok: false, text: result.message })
      }
    } catch (requestError: unknown) {
      if (requestError instanceof PinRequiredError) onPinRequired()
      else setFeedback({ ok: false, text: 'Não foi possível falar com o servidor.' })
    } finally {
      sendingRef.current = false
    }
  }

  const fieldClass = 'rounded-lg border border-stone-400 bg-white px-3 py-3 text-base'

  return (
    <section className="mx-auto grid max-w-md grid-cols-[minmax(0,1fr)] gap-5 px-4 pb-28 pt-6">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Orçamentos</h1>
        <button type="button" onClick={onBack} className="rounded-lg border border-stone-400 bg-white px-3 py-2 text-sm">Voltar</button>
      </header>

      <div className="flex items-center justify-between">
        <button type="button" aria-label="Mês anterior" onClick={() => setMonth((value) => shiftMonth(value, -1))} className="rounded-lg px-3 py-2 text-xl">‹</button>
        <h2 className="font-semibold">{monthLabel(month)}</h2>
        <button type="button" aria-label="Próximo mês" onClick={() => setMonth((value) => shiftMonth(value, 1))} className="rounded-lg px-3 py-2 text-xl">›</button>
      </div>

      {error && <p role="alert" className="text-red-700">{error}</p>}
      {budgets === null && !error && <p className="text-stone-600">Carregando orçamentos...</p>}
      {budgets?.length === 0 && <p className="text-stone-600">Nenhum orçamento para este mês.</p>}
      {budgets?.map((budget) => (
        <div key={budget.id} className="rounded-lg bg-white p-3">
          <div className="flex justify-between text-sm"><span>{budget.category}</span><span>{currency.format(budget.consumedAmount)} / {currency.format(budget.limitAmount)}</span></div>
          <div className="mt-1 h-2 rounded bg-stone-200"><div className="h-2 rounded bg-emerald-700" style={{ width: `${Math.min(budget.consumedPercentage, 100)}%` }} /></div>
          <p className="mt-1 text-xs text-stone-500">{budget.consumedPercentage.toFixed(0)}% consumido</p>
        </div>
      ))}

      <form className="grid gap-3 rounded-xl bg-white p-4" onSubmit={create}>
        <h2 className="font-semibold">Novo orçamento</h2>
        <select aria-label="Categoria do orçamento" value={categoryId ?? ''} onChange={(event) => setCategoryId(event.target.value ? Number(event.target.value) : null)} className={fieldClass}>
          <option value="">Escolha uma categoria</option>
          {categories.filter((category) => !category.archived).map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
        </select>
        <input aria-label="Limite do orçamento" inputMode="decimal" placeholder="Limite (R$)" value={limit} onChange={(event) => setLimit(event.target.value)} className={fieldClass} />
        <button type="submit" disabled={!canCreateBudget(categoryId, limit)} className="rounded-lg bg-emerald-700 py-3 font-semibold text-white disabled:opacity-50">Criar orçamento</button>
        {feedback && <p role={feedback.ok ? 'status' : 'alert'} className={feedback.ok ? 'text-sm text-emerald-800' : 'text-sm text-red-700'}>{feedback.text}</p>}
      </form>
    </section>
  )
}
