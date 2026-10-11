import { useEffect, useState, type FormEvent } from 'react'
import { getJson, postJson, PinRequiredError } from '../api.ts'
import { canContribute, canCreateGoal, progressPercent } from '../lib/budgetsGoals.ts'

type Goal = { id: number; title: string; targetAmount: number; currentAmount: number }

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function Metas({ onBack, onPinRequired }: {
  onBack: () => void
  onPinRequired: () => void
}) {
  const [goals, setGoals] = useState<Goal[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)
  const [title, setTitle] = useState('')
  const [target, setTarget] = useState('')
  const [contributions, setContributions] = useState<Record<number, string>>({})
  const [feedback, setFeedback] = useState<{ ok: boolean; text: string } | null>(null)

  useEffect(() => {
    getJson<Goal[]>('/api/goals')
      .then((list) => { setGoals(list); setError(null) })
      .catch((requestError: unknown) => {
        if (requestError instanceof PinRequiredError) onPinRequired()
        else setError('Não foi possível carregar as metas.')
      })
  }, [reload, onPinRequired])

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canCreateGoal(title, target)) return
    try {
      const result = await postJson('/api/goals', { title: title.trim(), targetAmount: Number(target.replace(',', '.')) })
      if (result.ok) {
        setTitle('')
        setTarget('')
        setFeedback({ ok: true, text: 'Meta criada' })
        setReload((value) => value + 1)
      } else {
        setFeedback({ ok: false, text: result.message })
      }
    } catch (requestError: unknown) {
      if (requestError instanceof PinRequiredError) onPinRequired()
      else setFeedback({ ok: false, text: 'Não foi possível falar com o servidor.' })
    }
  }

  async function contribute(goal: Goal, event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const amount = contributions[goal.id] ?? ''
    if (!canContribute(amount)) return
    try {
      const result = await postJson(`/api/goals/${goal.id}/contributions`, { amount: Number(amount.replace(',', '.')) })
      if (result.ok) {
        setContributions((current) => ({ ...current, [goal.id]: '' }))
        setFeedback({ ok: true, text: 'Aporte registrado' })
        setReload((value) => value + 1)
      } else {
        setFeedback({ ok: false, text: result.message })
      }
    } catch (requestError: unknown) {
      if (requestError instanceof PinRequiredError) onPinRequired()
      else setFeedback({ ok: false, text: 'Não foi possível falar com o servidor.' })
    }
  }

  const fieldClass = 'rounded-lg border border-stone-400 bg-white px-3 py-3 text-base'

  return (
    <section className="mx-auto grid max-w-md grid-cols-[minmax(0,1fr)] gap-5 px-4 pb-28 pt-6">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Metas</h1>
        <button type="button" onClick={onBack} className="rounded-lg border border-stone-400 bg-white px-3 py-2 text-sm">Voltar</button>
      </header>

      {error && <p role="alert" className="text-red-700">{error}</p>}
      {goals === null && !error && <p className="text-stone-600">Carregando metas...</p>}
      {goals?.length === 0 && <p className="text-stone-600">Nenhuma meta cadastrada.</p>}
      {goals?.map((goal) => {
        const percent = progressPercent(goal.currentAmount, goal.targetAmount)
        return (
          <article key={goal.id} className="grid min-w-0 gap-2 rounded-lg bg-white p-3">
            <div className="flex flex-wrap justify-between gap-x-3 text-sm"><span className="min-w-0 break-words">{goal.title}</span><span className="min-w-0 break-words">{currency.format(goal.currentAmount)} / {currency.format(goal.targetAmount)}</span></div>
            <div className="h-2 rounded bg-stone-200"><div className="h-2 rounded bg-emerald-700" style={{ width: `${percent}%` }} /></div>
            <p className="text-xs text-stone-500">{percent.toFixed(0)}% da meta</p>
            <form className="flex min-w-0 gap-2" onSubmit={(event) => void contribute(goal, event)}>
              <input aria-label={`Aporte para ${goal.title}`} inputMode="decimal" placeholder="Aporte (R$)" value={contributions[goal.id] ?? ''} onChange={(event) => setContributions((current) => ({ ...current, [goal.id]: event.target.value }))} className={`${fieldClass} w-full min-w-0 flex-1`} />
              <button type="submit" className="rounded-lg bg-emerald-700 px-3 text-sm font-semibold text-white">Aportar</button>
            </form>
          </article>
        )
      })}

      <form className="grid gap-3 rounded-xl bg-white p-4" onSubmit={create}>
        <h2 className="font-semibold">Nova meta</h2>
        <input aria-label="Título da meta" placeholder="Título" value={title} onChange={(event) => setTitle(event.target.value)} className={fieldClass} />
        <input aria-label="Valor alvo" inputMode="decimal" placeholder="Valor alvo (R$)" value={target} onChange={(event) => setTarget(event.target.value)} className={fieldClass} />
        <button type="submit" disabled={!canCreateGoal(title, target)} className="rounded-lg bg-emerald-700 py-3 font-semibold text-white disabled:opacity-50">Criar meta</button>
        {feedback && <p role={feedback.ok ? 'status' : 'alert'} className={feedback.ok ? 'text-sm text-emerald-800' : 'text-sm text-red-700'}>{feedback.text}</p>}
      </form>
    </section>
  )
}
