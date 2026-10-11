import { test } from 'node:test'
import assert from 'node:assert/strict'
import { forProfile, monthLabel, shiftMonth, summarize, type Transaction } from './finance.ts'

function tx(overrides: Partial<Transaction>): Transaction {
  return {
    id: 1, type: 'EXPENSE', description: 'x', amount: 0, date: '2026-01-10', profileId: 1,
    categoryId: 1, accountId: 1, creditCardId: null, targetAccountId: null, installmentCount: null,
    recurring: false, ...overrides,
  }
}

test('summarizeSumsIncomeExpenseBalance', () => {
  const totals = summarize([
    tx({ type: 'INCOME', amount: 3000 }),
    tx({ type: 'EXPENSE', amount: 1200.5 }),
    tx({ type: 'EXPENSE', amount: 99.5 }),
  ])
  assert.deepEqual(totals, { income: 3000, expense: 1300, balance: 1700 })
})

test('profileFilterAppliesToTotals', () => {
  const all = [
    tx({ profileId: 1, type: 'INCOME', amount: 1000 }),
    tx({ profileId: 2, type: 'EXPENSE', amount: 400 }),
    tx({ profileId: 1, type: 'EXPENSE', amount: 100 }),
  ]
  assert.deepEqual(summarize(forProfile(all, 1)), { income: 1000, expense: 100, balance: 900 })
  assert.deepEqual(summarize(forProfile(all, null)), { income: 1000, expense: 500, balance: 500 })
})

test('shiftMonthAcrossYearAndLabel', () => {
  assert.equal(shiftMonth('2026-01', -1), '2025-12')
  assert.equal(shiftMonth('2025-12', 1), '2026-01')
  assert.equal(shiftMonth('2026-03', 9), '2026-12')
  assert.equal(monthLabel('2026-01'), 'Janeiro de 2026')
})

test('transfersAreListedButNotInTotals', () => {
  const transfer = tx({ type: 'TRANSFER', amount: 500 })
  const list = [tx({ type: 'INCOME', amount: 200 }), transfer]
  assert.ok(list.includes(transfer), 'a transferência continua na lista')
  assert.deepEqual(summarize(list), { income: 200, expense: 0, balance: 200 })
})

test('emptyMonthTotalsAreZero', () => {
  assert.deepEqual(summarize([]), { income: 0, expense: 0, balance: 0 })
})
