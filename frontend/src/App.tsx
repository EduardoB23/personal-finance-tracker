import { useEffect, useState } from 'react'
import './App.css'

type Profile = { id: number; name: string }
type Account = { id: number; name: string; type: string; balance: number }
type Goal = { id: number; title: string; targetAmount: number; currentAmount: number }
type BudgetSummary = {
  id: number
  category: string
  limitAmount: number
  consumedAmount: number
  consumedPercentage: number
}

type DashboardData = {
  profiles: Profile[]
  accounts: Account[]
  goals: Goal[]
  budgets: BudgetSummary[]
}

const currentMonth = new Intl.DateTimeFormat('sv-SE', { year: 'numeric', month: '2-digit' }).format(new Date())

async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (!response.ok) throw new Error('Não foi possível carregar os dados financeiros.')
  return response.json() as Promise<T>
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)
}

function App() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([
      getJson<Profile[]>('/api/profiles'),
      getJson<Account[]>('/api/accounts'),
      getJson<Goal[]>('/api/goals'),
      getJson<BudgetSummary[]>(`/api/budgets/summary?month=${currentMonth}`),
    ])
      .then(([profiles, accounts, goals, budgets]) => setData({ profiles, accounts, goals, budgets }))
      .catch((requestError: unknown) => setError(requestError instanceof Error ? requestError.message : 'Erro inesperado.'))
  }, [])

  async function exportData() {
    const response = await fetch('/api/backup/export')
    if (!response.ok) return
    const blob = await response.blob()
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = 'finance-backup.json'
    link.click()
    URL.revokeObjectURL(link.href)
  }

  if (error) {
    return <main className="app-shell state-screen"><p className="state-error">{error}</p></main>
  }

  if (!data) {
    return <main className="app-shell state-screen"><p className="loading">Carregando seu panorama financeiro...</p></main>
  }

  const totalBalance = data.accounts.reduce((total, account) => total + account.balance, 0)

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Personal Finance Tracker / {currentMonth}</p>
          <h1>Resumo familiar</h1>
          <p className="lede">Uma leitura clara do que entrou, do que está comprometido e do que segue em direção às suas metas.</p>
        </div>
        <button type="button" className="primary-btn" onClick={exportData}>Exportar dados</button>
      </header>

      <section className="hero-balance">
        <span>Saldo consolidado</span>
        <strong>{formatCurrency(totalBalance)}</strong>
      </section>

      <section className="cards-grid">
        {[
          { label: 'Perfis', value: data.profiles.length },
          { label: 'Contas', value: data.accounts.length },
          { label: 'Metas', value: data.goals.length },
          { label: 'Orçamentos', value: data.budgets.length },
        ].map((item) => (
          <article key={item.label} className="summary-card">
            <span>{item.label}</span>
            <strong>{item.value}</strong>
          </article>
        ))}
      </section>

      <section className="panel-grid">
        <article className="panel">
          <div className="panel-heading"><p className="section-kicker">Patrimônio líquido local</p><h2>Contas</h2></div>
          {data.accounts.length === 0 ? <p className="empty-state">Cadastre sua primeira conta para acompanhar o saldo aqui.</p> : (
            <ul className="data-list">
              {data.accounts.map((account) => <li key={account.id}><span>{account.name}<small>{account.type}</small></span><strong>{formatCurrency(account.balance)}</strong></li>)}
            </ul>
          )}
        </article>

        <article className="panel">
          <div className="panel-heading"><p className="section-kicker">Acompanhamento mensal</p><h2>Orçamento do mês</h2></div>
          {data.budgets.length === 0 ? <p className="empty-state">Nenhum orçamento definido para este mês.</p> : data.budgets.map((budget) => (
            <div className="progress-row" key={budget.id}>
              <div className="progress-label"><span>{budget.category}</span><small>{formatCurrency(budget.consumedAmount)} / {formatCurrency(budget.limitAmount)}</small></div>
              <div className="progress-bar"><i style={{ width: `${Math.min(budget.consumedPercentage, 100)}%` }} /></div>
              <small className="percentage">{budget.consumedPercentage.toFixed(0)}% consumido</small>
            </div>
          ))}
        </article>

        <article className="panel wide">
          <div className="panel-heading"><p className="section-kicker">Planos em movimento</p><h2>Metas</h2></div>
          {data.goals.length === 0 ? <p className="empty-state">Crie uma meta para transformar planos em progresso visível.</p> : (
            <ul className="goals-list">
              {data.goals.map((goal) => {
                const percentage = goal.targetAmount === 0 ? 0 : (goal.currentAmount / goal.targetAmount) * 100
                return <li key={goal.id}><div className="progress-label"><span>{goal.title}</span><small>{formatCurrency(goal.currentAmount)} / {formatCurrency(goal.targetAmount)}</small></div><div className="progress-bar"><i style={{ width: `${Math.min(percentage, 100)}%` }} /></div></li>
              })}
            </ul>
          )}
        </article>
      </section>
    </main>
  )
}

export default App
