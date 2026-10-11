import { test } from 'node:test'
import assert from 'node:assert/strict'
import { buildPaymentBody, canPay, defaultPaymentForm, unpaidInvoices, type Invoice, type PaymentForm } from './invoices.ts'

function invoice(overrides: Partial<Invoice>): Invoice {
  return {
    billingMonth: '2026-02', amount: 1000, paidAmount: 0, remainingAmount: 1000,
    dueDate: '2026-02-10', status: 'Aberta', payments: [], ...overrides,
  }
}

test('onlyUnpaidInvoicesAreShown', () => {
  const list = [
    invoice({ status: 'Aberta' }),
    invoice({ status: 'Parcial', paidAmount: 300, remainingAmount: 700 }),
    invoice({ status: 'Paga', paidAmount: 1000, remainingAmount: 0 }),
  ]
  assert.deepEqual(unpaidInvoices(list).map((item) => item.status), ['Aberta', 'Parcial'])
})

test('paymentDefaultsToRemaining', () => {
  const form = defaultPaymentForm(invoice({ remainingAmount: 700 }), '2026-02-05')
  assert.deepEqual(form, { amount: '700,00', accountId: null, date: '2026-02-05' })
})

test('paymentNeedsAccountAndValidAmount', () => {
  const base: PaymentForm = { amount: '700,00', accountId: 10, date: '2026-02-05' }
  assert.equal(canPay(base, 700), true)
  assert.equal(canPay({ ...base, accountId: null }, 700), false)
  assert.equal(canPay({ ...base, amount: '0' }, 700), false)
  assert.equal(canPay({ ...base, amount: '' }, 700), false)
  assert.equal(canPay({ ...base, amount: '700,01' }, 700), false)
  assert.equal(canPay({ ...base, amount: '100' }, 700), true)
})

test('paymentBodyCarriesProfileAndAmount', () => {
  assert.deepEqual(buildPaymentBody({ amount: '12,50', accountId: 10, date: '2026-02-05' }, 3), {
    accountId: 10, profileId: 3, amount: 12.5, date: '2026-02-05',
  })
})
