# Entrega 5 - Pagamento de fatura do cartão

Sources:

- `.tasks/pagamento-fatura.md` - critérios 1 a 28, decisões e respostas de 11/10/2026
- Linear COD-14 - fonte original
- `docs/decisions.md` D17, D18, D19, D34

Profile: `light` (default; o projeto não declara `AGENTS.md`).

## Out of scope

- Pagamento no extrato - D18 e resposta de 11/10/2026
- Editar um pagamento - estorno e novo pagamento cobrem a correção
- Telas de orçamento e metas - entrega 7
- Retry automático de envio - mesmo critério da entrega 4

## Landing

Pacotes: `account` (pagamento, fatura, estorno, endpoints), `transaction` (recusa de exclusão), `backup` (seção nova), `frontend/src/lib/invoices.ts`, `frontend/src/screens/Faturas.tsx`.

| One-way door | Literal shape | Alternative rejected |
| --- | --- | --- |
| Tabela `invoice_payments` | `V4__pagamento_fatura.sql`: `(id, invoice_id FK credit_card_invoices, account_id FK bank_accounts, profile_id FK profiles, amount NUMERIC(19,2) NOT NULL, paid_on DATE NOT NULL)` | novo `TransactionType.INVOICE_PAYMENT`: entra em extrato e totais |
| Status derivado | `remainingAmount` = `amount` − soma dos pagamentos; `status` = `Aberta` se pago = 0, `Paga` se restante = 0, `Parcial` caso contrário | coluna `status` gravada: duas fontes da verdade |
| Contrato | `GET /api/credit-cards/{cardId}/invoices` → `[{billingMonth, amount, paidAmount, remainingAmount, dueDate, status, payments: [{id, paidOn, accountId, profileId, amount}]}]`; `POST /api/credit-cards/{cardId}/invoices/{billingMonth}/payments` com `{accountId, profileId, amount, date}` → 201 com `{id, amount, paidOn, accountId, profileId}`; `DELETE /api/invoice-payments/{id}` → 204 | pagamento por id de fatura: o id só existe depois que a fatura é criada |
| Lock pessimista na fatura | `@Lock(PESSIMISTIC_WRITE)` em `findByCreditCardIdAndBillingMonth` e em `lockById`, usados por pagamento, estorno e exclusão de compra | checagem sem lock: dois pagamentos passam pelo mesmo restante |
| Seção de pagamentos no backup é opcional na importação | `BackupExport.invoicePayments` por último; ausente = lista vazia; as demais seções continuam obrigatórias | rejeitar backups antigos: o backup de hoje não tem a seção |
| Backup de pagamento referencia fatura por cartão e mês | `PaymentData(id, creditCardId, billingMonth, accountId, profileId, amount, paidOn)`; a importação resolve a fatura pelo cartão remapeado e pelo mês | id da fatura do banco de origem: a importação remapeia ids de fatura só por cartão e mês |

- Nada mais nesta mudança é difícil de reverter

## Checks

### S1 - Pagamento parcial e total da fatura (API) · ~7 arquivos · ~18 KB · ~5k

**C1** - Fatura de R$ 1.000,00 com pagamento de R$ 300,00 lista `paidAmount` 300.00, `remainingAmount` 700.00, `status` `Parcial`
Proof: `InvoicePaymentApiTest#partialPaymentLeavesRemaining`

**C2** - Pagamento do restante lista `remainingAmount` 0.00 e `status` `Paga`
Proof: `InvoicePaymentApiTest#paymentOfRemainingMarksPaid`

**C3** - Fatura sem pagamentos lista `status` `Aberta` e `paidAmount` 0.00
Proof: `InvoicePaymentApiTest#invoiceWithoutPaymentsIsAberta`

**C4** - Pagamento de R$ 300,00 debita a conta e aumenta `availableLimit` do cartão em R$ 300,00
Proof: `InvoicePaymentApiTest#paymentMovesAccountBalanceAndLimit`

**C5** - Pagamento não cria lançamento em `GET /api/transactions`
Proof: `InvoicePaymentApiTest#paymentIsNotATransaction`

**C6** - Valor zero ou negativo responde `400` com `messages.amount` = `Valor deve ser maior que zero`
Proof: `InvoicePaymentApiTest#zeroOrNegativeAmountIsRejected`

**C7** - Valor acima do restante responde `400` com `message` = `Valor maior que o saldo da fatura`, sem mudar saldo, limite ou pagamentos
Proof: `InvoicePaymentApiTest#overpaymentIsRejectedWithoutChanges`

**C8** - Cartão sem fatura no mês responde `400` com `message` = `Fatura não encontrada`
Proof: `InvoicePaymentApiTest#missingInvoiceIsRejected`

**C9** - Conta ou perfil inexistente responde `400` com `Conta não encontrada` ou `Perfil não encontrado`
Proof: `InvoicePaymentApiTest#unknownAccountOrProfileIsRejected`

**C10** - Compra nova no mês de uma fatura parcialmente paga aumenta `remainingAmount` e não muda `paidAmount`
Proof: `InvoicePaymentApiTest#purchaseAfterPartialPaymentRaisesRemaining`

**C11** - Dois pagamentos simultâneos de R$ 1.000,00 numa fatura de R$ 1.000,00: exatamente um `201` e um `400`
Proof: `InvoicePaymentConcurrencyTest#simultaneousFullPaymentsAcceptOnlyOne`

**C12** - `paidAmount` nunca passa do `amount` da fatura (invariante; garantida por C7 e C11)
Proof: `InvoicePaymentApiTest#overpaymentIsRejectedWithoutChanges` e `InvoicePaymentConcurrencyTest#simultaneousFullPaymentsAcceptOnlyOne`

### S2 - Exclusão de compra e estorno · ~3 arquivos · ~8 KB · ~2k

**C15** - Compra de R$ 150,00 numa fatura de R$ 1.000,00 com R$ 900,00 pagos não pode ser excluída: `400` com `message` = `Fatura já tem pagamento que impede esta exclusão`, sem mudanças
Proof: `InvoiceDeletionTest#purchaseDeletionRefusedWhenPaymentsWouldGoNegative`

**C16** - `DELETE /api/invoice-payments/{id}` responde `204`, devolve R$ 300,00 ao saldo, tira R$ 300,00 do limite e volta `remainingAmount` para R$ 1.000,00
Proof: `InvoiceDeletionTest#refundRestoresBalanceLimitAndRemaining`

**C17** - Id de pagamento inexistente responde `400` com `message` = `Pagamento não encontrado`
Proof: `InvoiceDeletionTest#refundUnknownIdIsRejected`

**C18** - Depois do estorno, a exclusão do C15 responde `204` e devolve R$ 150,00 ao limite
Proof: `InvoiceDeletionTest#purchaseDeletableAfterRefund`

### S3 - Faturas na tela · ~3 arquivos · ~10 KB · ~3k

**C19** - Resumo lista faturas não pagas com cartão, mês, `Restante R$ x` e status
Proof: `frontend/src/lib/invoices.test.ts#onlyUnpaidInvoicesAreShown`
Proof: `npm run e2e` - roteiro `faturas: lista com restante e status`

**C20** - Sem fatura em aberto, o Resumo mostra `Nenhuma fatura em aberto`
Proof: `npm run e2e` - roteiro `faturas: vazio mostra mensagem` (roda antes de qualquer compra no cartão)

**C21** - "Pagar" abre o formulário com o valor igual ao restante, data de hoje e conta obrigatória
Proof: `frontend/src/lib/invoices.test.ts#paymentDefaultsToRemaining`
Proof: `frontend/src/lib/invoices.test.ts#paymentNeedsAccountAndValidAmount`

**C22** - Pagamento salvo mostra `Pagamento registrado` e atualiza o restante
Proof: `npm run e2e` - roteiro `faturas: pagamento parcial atualiza o restante`

**C23** - `400` mostra a mensagem recebida e mantém o valor digitado
Proof: `npm run e2e` - roteiro `faturas: valor acima do restante fica na tela`

**C24** - Dois toques em Pagar geram um único pagamento
Proof: `npm run e2e` - roteiro `faturas: dois toques geram um pagamento`

**C25** - A fatura com pagamentos lista cada um com data, conta, valor e botão "Estornar"
Proof: `npm run e2e` - roteiro `faturas: pagamentos aparecem com estornar`

### S4 - Estorno na tela e celular · ~2 arquivos · ~3 KB · ~1k

**C26** - "Estornar" mostra `Estornar pagamento de R$ 300,00?` com `Cancelar` e `Estornar`, sem enviar antes de confirmar
Proof: `npm run e2e` - roteiro `faturas: estornar pede confirmação`

**C27** - Estorno confirmado mostra `Pagamento estornado` e atualiza restante e lista
Proof: `npm run e2e` - roteiro `faturas: estorno confirmado atualiza`

**C28** - Em 375px, a seção de faturas não tem rolagem horizontal e a navegação continua com três abas
Proof: `npm run e2e` - roteiro `faturas: sem rolagem horizontal`

### Backup (critérios 13 e 14, e a compatibilidade da Landing)

**C13** - JSON exportado e importado mantém pagamentos com valor, conta, perfil e data, e restante igual
Proof: `InvoicePaymentBackupTest#jsonRoundTripKeepsPayments`

**C14** - CSV exportado e importado: o mesmo resultado de C13
Proof: `InvoicePaymentBackupTest#csvRoundTripKeepsPayments`

**Compatibilidade** - Backup sem a seção `invoicePayments` importa
Proof: `InvoicePaymentBackupTest#backupWithoutPaymentsSectionStillImports`

## States

Ver `.tasks/pagamento-fatura.md`, seção States: Aberta → Parcial → Paga, e a nova compra no mês (10).

## Coverage

| Set (size) | Member -> proof | Unproven |
| --- | --- | --- |
| status da fatura (3) | Aberta C3 · Parcial C1 · Paga C2 | - |
| validação do pagamento (5 regras) | valor C6 · acima do restante C7 · fatura C8 · conta C9 · perfil C9 | - |
| formatos do backup (2) | JSON C13 · CSV C14 | - |
| pontos de exclusão de compra (2) | com pagamento C15 · depois do estorno C18 | - |
| estados de tela da seção (4) | vazio C20 · erro C23 · carregamento | carregamento: Unresolved 1 da task |

- Claims naming a status code or response shape: C1 (a C3 na API), C6, C7, C8, C9, C15, C17 - each has a proof that crosses MockMvc
- Claims about screens (C19, C20, C22 to C28) are proven by `npm run e2e` against a running backend

## Handoff

Single batch. Estimated reading: ~45 KB total (≈ 11k tokens), far under 150k. No handoff.

## Verificação

Feita na própria sessão de build (sem Verifier separado, como nas entregas 3 e 4). Provas executadas:

| Check | Resultado | Como |
| --- | --- | --- |
| C1 a C10 | PASS | `InvoicePaymentApiTest` (10 testes) |
| C11 | PASS | `InvoicePaymentConcurrencyTest#simultaneousFullPaymentsAcceptOnlyOne` (duas threads, transações reais, lock pessimista) |
| C12 | PASS | `InvoicePaymentApiTest#overpaymentIsRejectedWithoutChanges` e a concorrência acima |
| C13 | PASS | `InvoicePaymentBackupTest#jsonRoundTripKeepsPayments` |
| C14 | PASS | `InvoicePaymentBackupTest#csvRoundTripKeepsPayments` |
| Compatibilidade | PASS | `InvoicePaymentBackupTest#backupWithoutPaymentsSectionStillImports` |
| C15 | PASS | `InvoiceDeletionTest#purchaseDeletionRefusedWhenPaymentsWouldGoNegative` |
| C16 | PASS | `InvoiceDeletionTest#refundRestoresBalanceLimitAndRemaining` |
| C17 | PASS | `InvoiceDeletionTest#refundUnknownIdIsRejected` |
| C18 | PASS | `InvoiceDeletionTest#purchaseDeletableAfterRefund` |
| C19 | PASS | `invoices.test.ts#onlyUnpaidInvoicesAreShown` e roteiro `faturas: lista com restante e status` |
| C20 | PASS | roteiro `faturas: vazio mostra mensagem` (roda num servidor sem compras) |
| C21 | PASS | `invoices.test.ts#paymentDefaultsToRemaining`, `#paymentNeedsAccountAndValidAmount`; roteiro `faturas: pagar abre com o restante...` |
| C22 | PASS | roteiro `faturas: pagar abre com o restante e pagamento parcial atualiza` |
| C23 | PASS | roteiro `faturas: valor acima do restante fica na tela` |
| C24 | PASS | roteiro `faturas: dois toques geram um pagamento` (conta na API: +1) |
| C25 | PASS | roteiro `faturas: pagamentos aparecem com estornar` |
| C26 | PASS | roteiro `faturas: estornar pede confirmação` (API não recebe nada antes de confirmar) |
| C27 | PASS | roteiro `faturas: estorno confirmado atualiza` |
| C28 | PASS | roteiro `faturas: sem rolagem horizontal` |

Suíte de backend completa: 91/91 (`mvn package`). Frontend: 22/22 testes de lógica, build e lint limpos. Roteiros de tela: 26/26 num servidor limpo.

**Gaps declarados**

- Sem Verifier independente.
- A concorrência (C11) é provada com duas threads no H2. Não é prova de produção com outro banco.
- C20 só passa num servidor sem compras; o roteiro começa antes da primeira compra.

**Desvios registrados**

- O cliente não bloqueia valor acima do restante. Quem decide é a API, e a mensagem chega à tela (critério 23). Corrigi isso depois do primeiro roteiro falhar.
- A seção de pagamentos do backup é opcional na importação, para não rejeitar backups antigos. As demais seções continuam obrigatórias.
- O filtro `profileId` do extrato e o status da fatura são derivados, sem coluna gravada.
