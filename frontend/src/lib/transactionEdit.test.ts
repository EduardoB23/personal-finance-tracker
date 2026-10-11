import { test } from 'node:test'
import assert from 'node:assert/strict'
import { buildEditBody, formFromTransaction, isEditable } from './transactionEdit.ts'
import type { Transaction } from './finance.ts'

function tx(overrides: Partial<Transaction>): Transaction {
  return {
    id: 1, type: 'EXPENSE', description: 'Mercado', amount: 42.5, date: '2026-01-05', profileId: 3,
    categoryId: 7, accountId: 10, creditCardId: null, targetAccountId: null, installmentCount: null,
    installmentGroupId: null, recurrenceSourceId: null, recurring: false, ...overrides,
  }
}

test('onlyTheRecurringRuleIsNotEditable', () => {
  assert.equal(isEditable(tx({})), true)
  assert.equal(isEditable(tx({ installmentGroupId: 5, installmentCount: 3 })), true)
  assert.equal(isEditable(tx({ recurrenceSourceId: 2 })), true)
  assert.equal(isEditable(tx({ recurring: true })), false)
})

test('formCarriesCardOrAccountAsPaymentSource', () => {
  assert.deepEqual(formFromTransaction(tx({ creditCardId: 20, accountId: null })).pay, { kind: 'card', id: 20, label: '' })
  assert.deepEqual(formFromTransaction(tx({})).pay, { kind: 'account', id: 10, label: '' })
  assert.equal(formFromTransaction(tx({ amount: 42.5 })).amount, '42,50')
})

test('editBodyKeepsWhoRecordedIt', () => {
  const form = { ...formFromTransaction(tx({})), amount: '150,00' }
  const body = buildEditBody(form, tx({}), 'Alimentação')
  assert.equal(body.amount, 150)
  assert.equal(body.profileId, 3)
  assert.equal(body.categoryId, 7)
  assert.equal(body.accountId, 10)
})
