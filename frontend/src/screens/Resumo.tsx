import { useEffect, useState } from 'react'
import { getJson, PinRequiredError } from '../api.ts'
import { Faturas } from './Faturas.tsx'

type Account = { id: number; name: string; type: string; balance: number }
type Goal = { id: number; title: string; targetAmount: number; currentAmount: number }
type BudgetSummary = {
  id: number
  category: string
  limitAmount: number
  consumedAmount: number
  consumedPercentage: number
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function Resumo({ profileId, personName, currentMonth, onTrocar, onCadastros, onConfiguracoes, onPinRequired }: {
  profileId: number
  personName: string
  currentMonth: string
  onTrocar: () => void
  onCadastros: () => void
  onConfiguracoes: () => void
  onPinRequired: () => void
}) {
  const [backup, setBackup] = useState<{ lastError: string | null; lastSuccessAt: string | null } | null>(null)
  const [data, setData] = useState<{ accounts: Account[]; goals: Goal[]; budgets: BudgetSummary[] } | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getJson<{ lastError: string | null; lastSuccessAt: string | null }>('/api/backup/automatic-status')
      .then((status) => setBackup(status))
      .catch(() => setBackup(null))
    Promise.all([
      getJson<Account[]>('/api/accounts'),
      getJson<Goal[]>('/api/goals'),
      getJson<BudgetSummary[]>(`/api/budgets/summary?month=${currentMonth}`),
    ])
      .then(([accounts, goals, budgets]) => setData({ accounts, goals, budgets }))
      .catch((requestError: unknown) => {
        if (requestError instanceof PinRequiredError) onPinRequired()
        else setError(requestError instanceof Error ? requestError.message : 'Erro inesperado.')
      })
  }, [currentMonth, onPinRequired])

  if (error) return <p role="alert" className="p-4 text-red-700">{error}</p>
  if (!data) return <p className="p-4 text-stone-600">Carregando seu panorama financeiro...</p>

  const totalBalance = data.accounts.reduce((total, account) => total + account.balance, 0)

  return (
    <section className="mx-auto grid max-w-md gap-4 px-4 pb-28 pt-6">
      <header className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm text-stone-600">Você: {personName}</p>
          <h1 className="text-2xl font-semibold">Resumo familiar</h1>
        </div>
        <button type="button" onClick={onTrocar} className="shrink-0 rounded-lg border border-stone-400 bg-white px-3 py-2 text-sm">Trocar</button>
      </header>

      <div className="rounded-xl bg-white p-4">
        <span className="text-sm text-stone-600">Saldo consolidado</span>
        <strong className="block text-3xl">{currency.format(totalBalance)}</strong>
      </div>

      <div className="rounded-xl bg-white p-4">
        <h2 className="mb-2 font-semibold">Contas</h2>
        {data.accounts.length === 0 ? (
          <p className="text-stone-600">Nenhuma conta cadastrada.</p>
        ) : (
          <ul className="grid gap-2">
            {data.accounts.map((account) => (
              <li key={account.id} className="flex justify-between gap-2">
                <span>{account.name}</span>
                <strong>{currency.format(account.balance)}</strong>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="rounded-xl bg-white p-4">
        <h2 className="mb-2 font-semibold">Orçamento do mês</h2>
        {data.budgets.length === 0 ? (
          <p className="text-stone-600">Nenhum orçamento definido para este mês.</p>
        ) : (
          data.budgets.map((budget) => (
            <div key={budget.id} className="mb-3">
              <div className="flex justify-between text-sm"><span>{budget.category}</span><span>{currency.format(budget.consumedAmount)} / {currency.format(budget.limitAmount)}</span></div>
              <div className="mt-1 h-2 rounded bg-stone-200"><div className="h-2 rounded bg-emerald-700" style={{ width: `${Math.min(budget.consumedPercentage, 100)}%` }} /></div>
            </div>
          ))
        )}
      </div>

      <div className="rounded-xl bg-white p-4">
        <h2 className="mb-2 font-semibold">Metas</h2>
        {data.goals.length === 0 ? (
          <p className="text-stone-600">Crie uma meta para acompanhar seus planos.</p>
        ) : (
          data.goals.map((goal) => {
            const percentage = goal.targetAmount === 0 ? 0 : (goal.currentAmount / goal.targetAmount) * 100
            return (
              <div key={goal.id} className="mb-3">
                <div className="flex justify-between text-sm"><span>{goal.title}</span><span>{currency.format(goal.currentAmount)} / {currency.format(goal.targetAmount)}</span></div>
                <div className="mt-1 h-2 rounded bg-stone-200"><div className="h-2 rounded bg-emerald-700" style={{ width: `${Math.min(percentage, 100)}%` }} /></div>
              </div>
            )
          })
        )}
      </div>

      {backup?.lastError && (
        <p role="alert" className="rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-800">
          Backup automático falhou: {backup.lastError}. Último backup: {backup.lastSuccessAt ? backup.lastSuccessAt.replace('T', ' ').slice(0, 16) : 'nenhum'}.
        </p>
      )}

      <Faturas profileId={profileId} accounts={data.accounts} onPinRequired={onPinRequired} />

      <div className="grid grid-cols-2 gap-2">
        <button type="button" onClick={onCadastros} className="rounded-lg border border-stone-400 bg-white py-3 text-sm font-medium">Cadastros</button>
        <button type="button" onClick={onConfiguracoes} className="rounded-lg border border-stone-400 bg-white py-3 text-sm font-medium">Configurações</button>
      </div>
    </section>
  )
}
