// Regras do extrato: meses, totais e filtro por pessoa. Puro, para ser testado sem navegador.

export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER'

export type Transaction = {
  id: number
  type: TransactionType
  description: string
  amount: number
  date: string
  profileId: number
  categoryId: number | null
  accountId: number | null
  creditCardId: number | null
  targetAccountId: number | null
  installmentCount: number | null
  recurring: boolean
}

export type Totals = { income: number; expense: number; balance: number }

// Formata uma data como AAAA-MM-DD usando as partes locais, sem passar pelo UTC
function toIsoDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function todayIso(now: Date = new Date()): string {
  return toIsoDate(now)
}

export function currentMonth(now: Date = new Date()): string {
  return toIsoDate(now).slice(0, 7)
}

export function shiftMonth(month: string, delta: number): string {
  const [year, monthNumber] = month.split('-').map(Number)
  const shifted = new Date(year, monthNumber - 1 + delta, 1)
  return `${shifted.getFullYear()}-${String(shifted.getMonth() + 1).padStart(2, '0')}`
}

export function monthLabel(month: string): string {
  const [year, monthNumber] = month.split('-').map(Number)
  const label = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })
    .format(new Date(year, monthNumber - 1, 1))
  return label.charAt(0).toUpperCase() + label.slice(1)
}

export function forProfile(transactions: Transaction[], profileId: number | null): Transaction[] {
  if (profileId === null) return transactions
  return transactions.filter((transaction) => transaction.profileId === profileId)
}

// Transferência não é entrada nem saída (D18): fica na lista, fora dos totais
export function summarize(transactions: Transaction[]): Totals {
  let income = 0
  let expense = 0
  for (const transaction of transactions) {
    if (transaction.type === 'INCOME') income += transaction.amount
    if (transaction.type === 'EXPENSE') expense += transaction.amount
  }
  return { income, expense, balance: income - expense }
}
