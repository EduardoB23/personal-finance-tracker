import { test } from 'node:test'
import assert from 'node:assert/strict'
import { buildBody, canSubmit, emptyForm, payOptions, type QuickForm } from './quickEntry.ts'

const accounts = [{ id: 10, name: 'Conjunta' }]
const cards = [{ id: 20, name: 'Nubank' }]

function filled(overrides: Partial<QuickForm>): QuickForm {
  return { ...emptyForm('2026-10-11'), amount: '42,50', categoryId: 3, pay: { kind: 'account', id: 10, label: 'Conjunta' }, ...overrides }
}

test('initialFormDefaults', () => {
  const form = emptyForm('2026-10-11')
  assert.equal(form.type, 'EXPENSE')
  assert.equal(form.date, '2026-10-11')
  assert.equal(form.amount, '')
  assert.equal(form.categoryId, null)
  assert.equal(form.pay, null)
})

test('expensePayloadUsesAccount', () => {
  const body = buildBody(filled({}), 1, 'Alimentação')
  assert.deepEqual(body, {
    type: 'EXPENSE', description: 'Alimentação', amount: 42.5, date: '2026-10-11',
    profileId: 1, categoryId: 3, accountId: 10, recurring: false,
  })
})

test('cannotSubmitWithoutPositiveAmount', () => {
  assert.equal(canSubmit(filled({ amount: '' })), false)
  assert.equal(canSubmit(filled({ amount: '0' })), false)
  assert.equal(canSubmit(filled({ amount: 'abc' })), false)
  assert.equal(canSubmit(filled({ amount: '0,01' })), true)
})

test('incomeOffersOnlyAccounts', () => {
  const options = payOptions('INCOME', accounts, cards)
  assert.deepEqual(options.map((option) => option.kind), ['account'])
})

test('expenseOffersAccountsAndCards', () => {
  const options = payOptions('EXPENSE', accounts, cards)
  assert.deepEqual(options.map((option) => option.kind), ['account', 'card'])
  assert.equal(options[1].label, 'Nubank (cartão)')
})

test('cardSelectionSendsCreditCardId', () => {
  const body = buildBody(filled({ pay: { kind: 'card', id: 20, label: 'Nubank (cartão)' } }), 1, 'Alimentação')
  assert.equal(body.creditCardId, 20)
  assert.equal('accountId' in body, false)
})

test('installmentPayloadCarriesCount', () => {
  const body = buildBody(filled({ pay: { kind: 'card', id: 20, label: 'Nubank (cartão)' }, installments: 3 }), 1, 'Alimentação')
  assert.equal(body.installmentCount, 3)
})

test('blankDescriptionUsesCategoryName', () => {
  assert.equal(buildBody(filled({ description: '   ' }), 1, 'Lazer').description, 'Lazer')
  assert.equal(buildBody(filled({ description: ' Cinema ' }), 1, 'Lazer').description, 'Cinema')
})

test('transferSendsTargetAndNoCategory', () => {
  const body = buildBody(filled({ type: 'TRANSFER', categoryId: null, targetAccountId: 11 }), 1, '')
  assert.equal(body.targetAccountId, 11)
  assert.equal(body.description, 'Transferência')
  assert.equal('categoryId' in body, false)
  assert.equal(canSubmit(filled({ type: 'TRANSFER', categoryId: null, targetAccountId: null })), false)
})
