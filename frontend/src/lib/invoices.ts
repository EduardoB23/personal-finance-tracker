// Regras das faturas: quais aparecem, valor padrão do pagamento e corpo enviado à API. Puro, para teste sem navegador.

export type Payment = { id: number; paidOn: string; accountId: number; profileId: number; amount: number }

export type Invoice = {
  billingMonth: string
  amount: number
  paidAmount: number
  remainingAmount: number
  dueDate: string
  status: 'Aberta' | 'Parcial' | 'Paga'
  payments: Payment[]
}

export type PaymentForm = { amount: string; accountId: number | null; date: string }

// Fatura paga some da lista de pendências; a seção ainda mostra pagamentos das parciais
export function unpaidInvoices(invoices: Invoice[]): Invoice[] {
  return invoices.filter((invoice) => invoice.status !== 'Paga')
}

export function defaultPaymentForm(invoice: Invoice, today: string): PaymentForm {
  return { amount: invoice.remainingAmount.toFixed(2).replace('.', ','), accountId: null, date: today }
}

export function parsePaymentAmount(value: string): number {
  return Number(value.trim().replace(',', '.'))
}

// Valor acima do restante não é bloqueado aqui: a API responde a mensagem e a tela a mostra (critério 23)
export function canSend(form: PaymentForm): boolean {
  const amount = parsePaymentAmount(form.amount)
  return Number.isFinite(amount) && amount > 0 && form.accountId !== null
}

export function canPay(form: PaymentForm, remaining: number): boolean {
  return canSend(form) && parsePaymentAmount(form.amount) <= remaining + 0.000001
}

export function buildPaymentBody(form: PaymentForm, profileId: number) {
  return {
    accountId: form.accountId,
    profileId,
    amount: parsePaymentAmount(form.amount),
    date: form.date,
  }
}
