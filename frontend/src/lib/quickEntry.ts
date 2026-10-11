// Regras do lançamento rápido: opções de "pagar com", estado inicial e corpo enviado à API.

import type { TransactionType } from './finance.ts'

export type Account = { id: number; name: string }
export type Card = { id: number; name: string }

export type PayOption = { kind: 'account' | 'card'; id: number; label: string }

export type QuickForm = {
  type: TransactionType
  amount: string
  categoryId: number | null
  pay: PayOption | null
  date: string
  description: string
  installments: number
  recurring: boolean
  targetAccountId: number | null
}

export type TransactionBody = {
  type: TransactionType
  description: string
  amount: number
  date: string
  profileId: number
  categoryId?: number
  accountId?: number
  creditCardId?: number
  targetAccountId?: number
  installmentCount?: number
  recurring: boolean
}

export function emptyForm(today: string): QuickForm {
  return {
    type: 'EXPENSE',
    amount: '',
    categoryId: null,
    pay: null,
    date: today,
    description: '',
    installments: 1,
    recurring: false,
    targetAccountId: null,
  }
}

// Receita e transferência só saem de conta; cartão aceita apenas despesa (regra da API)
export function payOptions(type: TransactionType, accounts: Account[], cards: Card[]): PayOption[] {
  const accountOptions: PayOption[] = accounts.map((account) => ({ kind: 'account', id: account.id, label: account.name }))
  if (type !== 'EXPENSE') return accountOptions
  const cardOptions: PayOption[] = cards.map((card) => ({ kind: 'card', id: card.id, label: `${card.name} (cartão)` }))
  return [...accountOptions, ...cardOptions]
}

export function parseAmount(value: string): number {
  return Number(value.trim().replace(',', '.'))
}

export function canSubmit(form: QuickForm): boolean {
  const amount = parseAmount(form.amount)
  if (!Number.isFinite(amount) || amount <= 0) return false
  if (form.pay === null) return false
  if (form.type === 'TRANSFER') return form.targetAccountId !== null
  return form.categoryId !== null
}

export function buildBody(form: QuickForm, profileId: number, categoryName: string): TransactionBody {
  const body: TransactionBody = {
    type: form.type,
    description: form.description.trim() || (form.type === 'TRANSFER' ? 'Transferência' : categoryName),
    amount: parseAmount(form.amount),
    date: form.date,
    profileId,
    recurring: form.recurring,
  }
  if (form.type !== 'TRANSFER') body.categoryId = form.categoryId ?? undefined
  if (form.pay?.kind === 'card') {
    body.creditCardId = form.pay.id
    body.installmentCount = form.installments
  } else if (form.pay) {
    body.accountId = form.pay.id
  }
  if (form.type === 'TRANSFER') body.targetAccountId = form.targetAccountId ?? undefined
  return body
}
