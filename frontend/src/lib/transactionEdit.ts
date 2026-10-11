// Edição de lançamento simples: quais podem ser editadas e como o formulário nasce do lançamento.
import { buildBody, type QuickForm } from './quickEntry.ts'
import type { Transaction } from './finance.ts'

// Avulso, parcela e ocorrência recorrente são editáveis, uma a uma. A regra recorrente não é (entrega 7)
export function isEditable(transaction: Transaction): boolean {
  return !transaction.recurring
}

export function formFromTransaction(transaction: Transaction): QuickForm {
  const pay = transaction.creditCardId !== null
    ? { kind: 'card' as const, id: transaction.creditCardId, label: '' }
    : transaction.accountId !== null
      ? { kind: 'account' as const, id: transaction.accountId, label: '' }
      : null
  return {
    type: transaction.type,
    amount: transaction.amount.toFixed(2).replace('.', ','),
    categoryId: transaction.categoryId,
    pay,
    date: transaction.date,
    description: transaction.description,
    installments: 1,
    recurring: false,
    targetAccountId: transaction.targetAccountId,
  }
}

export function buildEditBody(form: QuickForm, transaction: Transaction, categoryName: string) {
  return buildBody(form, transaction.profileId, categoryName)
}
