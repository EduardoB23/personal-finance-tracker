import { useEffect, useRef, useState, type FormEvent } from 'react'
import { getJson, postJson, PinRequiredError } from '../api.ts'
import { buildPaymentBody, canSend, defaultPaymentForm, unpaidInvoices, type Invoice, type PaymentForm } from '../lib/invoices.ts'
import { monthLabel, todayIso } from '../lib/finance.ts'
import type { Account } from '../lib/quickEntry.ts'

type Card = { id: number; name: string }

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

type CardInvoices = { card: Card; invoices: Invoice[] }
type Feedback = { ok: boolean; text: string }

export function Faturas({ profileId, accounts, onPinRequired }: {
  profileId: number
  accounts: Account[]
  onPinRequired: () => void
}) {
  const [cards, setCards] = useState<CardInvoices[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reload, setReload] = useState(0)
  const [payingKey, setPayingKey] = useState<string | null>(null)
  const [form, setForm] = useState<PaymentForm | null>(null)
  const [sending, setSending] = useState(false)
  const [feedback, setFeedback] = useState<Feedback | null>(null)
  const [confirmingId, setConfirmingId] = useState<number | null>(null)
  // Guarda síncrona contra dois toques antes do re-render (critério 24)
  const sendingRef = useRef(false)

  useEffect(() => {
    let active = true
    getJson<Card[]>('/api/credit-cards')
      .then((list) => Promise.all(list.map((card) =>
        getJson<Invoice[]>(`/api/credit-cards/${card.id}/invoices`).then((invoices) => ({ card, invoices })))))
      .then((loaded) => {
        if (active) {
          setCards(loaded)
          setError(null)
        }
      })
      .catch((requestError: unknown) => {
        if (!active) return
        if (requestError instanceof PinRequiredError) onPinRequired()
        else setError('Não foi possível carregar as faturas.')
      })
    return () => {
      active = false
    }
  }, [reload, onPinRequired])

  if (error) return <p role="alert" className="text-red-700">{error}</p>

  const accountName = (id: number) => accounts.find((account) => account.id === id)?.name ?? ''
  const pendingCount = (cards ?? []).reduce((total, entry) => total + unpaidInvoices(entry.invoices).length, 0)

  function openPayment(key: string, invoice: Invoice) {
    setPayingKey(key)
    setForm(defaultPaymentForm(invoice, todayIso()))
    setFeedback(null)
  }

  async function pay(event: FormEvent<HTMLFormElement>, card: Card, invoice: Invoice) {
    event.preventDefault()
    if (!form || sendingRef.current || !canSend(form)) return
    sendingRef.current = true
    setSending(true)
    setFeedback(null)
    try {
      const url = `/api/credit-cards/${card.id}/invoices/${invoice.billingMonth}/payments`
      const result = await postJson(url, buildPaymentBody(form, profileId))
      if (result.ok) {
        setPayingKey(null)
        setForm(null)
        setFeedback({ ok: true, text: 'Pagamento registrado' })
        setReload((value) => value + 1)
      } else {
        setFeedback({ ok: false, text: result.message })
      }
    } catch (requestError: unknown) {
      if (requestError instanceof PinRequiredError) onPinRequired()
      else setFeedback({ ok: false, text: 'Não foi possível falar com o servidor.' })
    } finally {
      sendingRef.current = false
      setSending(false)
    }
  }

  async function refund(paymentId: number) {
    if (sendingRef.current) return
    sendingRef.current = true
    setSending(true)
    try {
      const response = await fetch(`/api/invoice-payments/${paymentId}`, { method: 'DELETE' })
      if (response.status === 401) throw new PinRequiredError()
      if (response.ok) {
        setConfirmingId(null)
        setFeedback({ ok: true, text: 'Pagamento estornado' })
        setReload((value) => value + 1)
      } else {
        const body = (await response.json().catch(() => ({}))) as { message?: string }
        setFeedback({ ok: false, text: body.message ?? 'Não foi possível estornar.' })
      }
    } catch (requestError: unknown) {
      if (requestError instanceof PinRequiredError) onPinRequired()
      else setFeedback({ ok: false, text: 'Não foi possível falar com o servidor.' })
    } finally {
      sendingRef.current = false
      setSending(false)
    }
  }

  const fieldClass = 'rounded-lg border border-stone-400 bg-white px-3 py-2 text-base'

  return (
    <section aria-label="Faturas" className="grid gap-3 rounded-xl bg-white p-4">
      <h2 className="font-semibold">Faturas</h2>

      {cards === null ? (
        <p className="text-stone-600">Carregando faturas...</p>
      ) : pendingCount === 0 ? (
        <p className="text-stone-600">Nenhuma fatura em aberto</p>
      ) : (
        cards.map(({ card, invoices }) => unpaidInvoices(invoices).map((invoice) => {
          const key = `${card.id}:${invoice.billingMonth}`
          const paying = payingKey === key
          return (
            <article key={key} className="grid gap-2 rounded-lg border border-stone-300 p-3">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="font-medium">{card.name} · {monthLabel(invoice.billingMonth)}</p>
                  <p className="text-sm text-stone-600">{invoice.status} · Restante {currency.format(invoice.remainingAmount)}</p>
                </div>
                {!paying && (
                  <button type="button" onClick={() => openPayment(key, invoice)} className="shrink-0 rounded-lg bg-emerald-700 px-3 py-2 text-sm font-semibold text-white">
                    Pagar
                  </button>
                )}
              </div>

              {paying && form && (
                <form noValidate className="grid gap-2" onSubmit={(event) => void pay(event, card, invoice)}>
                  <label className="grid gap-1 text-sm font-medium">
                    Valor (R$)
                    <input inputMode="decimal" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} className={fieldClass} />
                  </label>
                  <label className="grid gap-1 text-sm font-medium">
                    Pagar com a conta
                    <select value={form.accountId ?? ''} onChange={(event) => setForm({ ...form, accountId: event.target.value ? Number(event.target.value) : null })} className={fieldClass}>
                      <option value="">Escolha uma conta</option>
                      {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
                    </select>
                  </label>
                  <label className="grid gap-1 text-sm font-medium">
                    Data
                    <input type="date" value={form.date} onChange={(event) => setForm({ ...form, date: event.target.value })} className={fieldClass} />
                  </label>
                  <div className="flex gap-2">
                    <button type="submit" disabled={sending} className="flex-1 rounded-lg bg-emerald-700 py-2 font-semibold text-white disabled:opacity-50">Pagar</button>
                    <button type="button" onClick={() => { setPayingKey(null); setForm(null) }} className="rounded-lg border border-stone-400 px-3 py-2 text-sm">Cancelar</button>
                  </div>
                </form>
              )}
            </article>
          )
        }))
      )}

      {cards?.some(({ invoices }) => invoices.some((invoice) => invoice.payments.length > 0)) && (
        <div className="grid gap-2">
          <h3 className="text-sm font-semibold text-stone-600">Pagamentos</h3>
          {cards.flatMap(({ card, invoices }) => invoices.flatMap((invoice) => invoice.payments.map((payment) => (
            <div key={payment.id} className="grid gap-1 rounded-lg bg-stone-50 p-2 text-sm">
              <div className="flex justify-between gap-2">
                <span>{payment.paidOn.split('-').reverse().join('/')} · {accountName(payment.accountId)} · {card.name}</span>
                <strong>{currency.format(payment.amount)}</strong>
              </div>
              {confirmingId === payment.id ? (
                <div className="grid gap-1">
                  <p>Estornar pagamento de {currency.format(payment.amount)}?</p>
                  <div className="flex gap-2">
                    <button type="button" onClick={() => setConfirmingId(null)} className="rounded-lg border border-stone-400 px-3 py-1">Cancelar</button>
                    <button type="button" disabled={sending} onClick={() => void refund(payment.id)} className="rounded-lg bg-red-700 px-3 py-1 text-white disabled:opacity-50">Estornar</button>
                  </div>
                </div>
              ) : (
                <button type="button" onClick={() => setConfirmingId(payment.id)} className="justify-self-start text-red-700 underline">Estornar</button>
              )}
            </div>
          ))))}
        </div>
      )}

      {feedback && <p role={feedback.ok ? 'status' : 'alert'} className={feedback.ok ? 'text-sm text-emerald-800' : 'text-sm text-red-700'}>{feedback.text}</p>}
    </section>
  )
}
