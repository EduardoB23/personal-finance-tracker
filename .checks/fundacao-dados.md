# Entrega 2 - Fundação de dados (COD-11)

Sources:

- `.tasks/fundacao-dados.md` - critérios 1 a 27, escopo, decisões e pontos abertos
- `docs/decisions.md` - D12, D14 a D17, D19, D21, D25, D28, D29
- Linear COD-11 - issue da entrega
- conversa de 10/10/2026 - "1 - a" (contas da família), "2 - a" (recorrência), "recusar duplicado" (critério 11), "task só"

## Out of scope

- Telas (entrega 4), PIN (entrega 3), pagamento de fatura (entrega 5), edição de lançamentos (entrega 7)
- Parcelamento em conta sem cartão continua debitando tudo na criação (Unresolved 3 da task)
- Metas continuam vinculadas a perfil (Unresolved 5 da task)

## Landing

Novo módulo `category`. `Transaction`, `Budget`, `BankAccount` e `CreditCard` mudam de forma; `TransactionService` concentra o efeito de um lançamento num único método usado pela criação, pela recorrência e pelo estorno; `CreditCardInvoiceService` ganha a operação inversa de `addPurchase`. O backup passa a ler o CSV pelo nome das colunas do cabeçalho, em vez de posições fixas. As relações JPA nas entidades expostas pela API ficam fora do JSON, que passa a mostrar só os ids (`categoryId`, `profileId`...).

| One-way door | Literal shape | Alternative rejected |
| --- | --- | --- |
| Flyway como único mecanismo de schema | `flyway-core`; `db/migration/V1__baseline.sql`, `V2__default_categories.sql`; `ddl-auto: validate`; remove `TransactionSchemaMigration` | `ddl-auto: update` - sem histórico, não remove colunas |
| Tabela de categorias | `categories(id, name, type, archived)` + coluna gerada `name_key = LOWER(name)` com `UNIQUE(name_key, type)` (o H2 não aceita índice sobre expressão) | texto livre; enum |
| Categoria nas transações e orçamentos | `transactions.category_id` FK nula só em `TRANSFER`; `budgets.category_id` FK not null | coluna texto em paralelo |
| Contas e cartões sem dono | `bank_accounts` e `credit_cards` sem `profile_id`; `Profile` perde as coleções `accounts`, `cards`, `budgets` e `transactions` (fica só `goals`) | manter o dono (D28) |
| Orçamento da família, único por categoria e mês | `budgets` sem `profile_id`, `UNIQUE(category_id, budget_month)` | orçamento por perfil (D16) |
| Vínculo ocorrência → regra | `transactions.recurrence_source_id` FK `ON DELETE SET NULL`, `UNIQUE(recurrence_source_id, "DATE")` | comparação de campos |
| Ocorrências excluídas | `recurrence_exceptions(source_transaction_id FK ON DELETE CASCADE, occurrence_date)`, `UNIQUE` no par | flag de exclusão lógica |
| Grupo de parcelas | `transactions.installment_group_id` = `id` da parcela 1 | sufixo "Parcela i/N" na descrição |
| Fatura única por cartão e mês | `UNIQUE(credit_card_id, billing_month)` em `credit_card_invoices` (o código já assumia isso em `findByCreditCardIdAndBillingMonth`) | sem restrição - duas faturas no mesmo mês quebrariam o `Optional` |
| Formato do backup | JSON: `categories[]`, `recurrenceExceptions[]`, transações com `categoryId`/`installmentGroupId`/`recurrenceSourceId`, sem `profileId` em contas/cartões/orçamentos. CSV: cabeçalho com nomes de coluna, uma linha por registro com `entity` em `PROFILE`, `CATEGORY`, `ACCOUNT`, `CARD`, `INVOICE`, `BUDGET`, `GOAL`, `TRANSACTION`, `RECURRENCE_EXCEPTION` | CSV posicional de 23 colunas - ilegível e frágil |
| JSON das entidades na API | `Transaction` expõe `categoryId`, `profileId`, `accountId`, `targetAccountId`, `creditCardId`, `installmentGroupId`, `recurrenceSourceId` em vez de objetos aninhados; `Budget` expõe `categoryId` | objetos aninhados com carregamento preguiçoso - serialização frágil e payload maior |

- Lançamento recorrente e parcelado ao mesmo tempo passa a ser recusado (`400`, "Lançamento recorrente não pode ser parcelado"): guarda contra um caso que geraria uma recorrência por parcela. Reversível.
- Nada mais nesta mudança é difícil de reverter

## Checks

### S1 - Categorias cadastráveis · 8 arquivos · ~20 KB · ~5k

**C1** - Banco vazio: `GET /api/categories` → `200` com as 9 categorias padrão (7 `EXPENSE`, 2 `INCOME`), todas `archived=false`
Proof: `CategoryApiTest.defaultCategoriesAreSeeded`

**C2** - `POST /api/categories` `{"name":"Pets","type":"EXPENSE"}` → `201`; aparece em `?type=EXPENSE` e não em `?type=INCOME`
Proof: `CategoryApiTest.createdCategoryIsListedByType`

**C3** - Nome repetido no mesmo tipo, sem diferenciar maiúsculas → `400` `Categoria já existe`
Proof: `CategoryApiTest.duplicateCategoryIsRejected`

**C4** - `DELETE /api/categories/{id}` → `204`; some da lista; aparece com `includeArchived=true` e `archived=true`; lançamento antigo mantém o `categoryId`
Proof: `CategoryApiTest.deletedCategoryIsArchived`

**C5** - `categoryId` inexistente, arquivado ou de outro tipo → `400` `Categoria inválida para este lançamento`
Proof: `CategoryApiTest.invalidCategoryIsRejected`

**C6** - `TRANSFER` sem `categoryId` → `201`; `EXPENSE`/`INCOME` sem `categoryId` → `400` com `messages.categoryId`
Proof: `CategoryApiTest.categoryIsRequiredExceptForTransfers`

### S2 - Contas e cartões da família · 8 arquivos · ~15 KB · ~4k

**C7** - `POST /api/accounts` e `POST /api/credit-cards` sem `profileId` → `201`; as listas não têm campo de perfil
Proof: `FamilyAccountsTest.accountsAndCardsHaveNoOwner`

**C8** - Maria e João lançam na mesma conta e no mesmo cartão → `201` cada, com o `profileId` de quem lançou
Proof: `FamilyAccountsTest.anyProfileUsesAnyAccountOrCard`

### S3 - Orçamento da família · 5 arquivos · ~10 KB · ~3k

**C9** - Orçamento R$ 500,00 de Alimentação em 2026-01; R$ 200 (Maria, conta, 10/01) + R$ 100 (João, cartão, 12/01) + R$ 50 (05/02) → `consumedAmount=300.00`, `consumedPercentage=60.00`
Proof: `FamilyBudgetTest.summaryAddsEveryProfileInTheMonth`

**C10** - Compra de R$ 300,00 em 3x no cartão em 10/01/2026 → R$ 100,00 consumidos em 2026-01, 2026-02 e 2026-03
Proof: `FamilyBudgetTest.cardInstallmentsCountInTheirOwnMonth`

**C11** - Segundo orçamento para a mesma categoria e mês → `400` `Já existe orçamento para esta categoria neste mês`
Proof: `FamilyBudgetTest.duplicateBudgetIsRejected`

### S4 - Recorrência que move o saldo · 6 arquivos · ~25 KB · ~6k

**C12** - Despesa recorrente R$ 900,00 em 15/01/2026, saldo R$ 1.000,00, processada até 15/03/2026 → 3 lançamentos, fevereiro e março com `recurrenceSourceId` = regra, saldo R$ -1.700,00
Proof: `RecurringTransactionSchedulerTest.dueOccurrencesApplyTheirEffect`

**C13** - Processar duas vezes até a mesma data → nada novo, saldo igual
Proof: `RecurringTransactionSchedulerTest.processingTwiceChangesNothing`

**C14** - Assinatura R$ 40,00 no cartão em 05/01/2026 processada até 05/02/2026 → 2 lançamentos, cada um na fatura do seu mês, limite disponível -R$ 80,00
Proof: `RecurringTransactionSchedulerTest.cardRecurrenceGoesToEachInvoice`

**C15** - Data de processamento posterior a hoje → nenhuma ocorrência com data posterior a hoje
Proof: `RecurringTransactionSchedulerTest.futureOccurrencesAreNotCreated`

**C16** - Na inicialização (`ApplicationReadyEvent`) as ocorrências vencidas são geradas
Proof: `RecurringTransactionSchedulerTest.startupProcessesDueOccurrences`

### S5 - Exclusão com estorno · 7 arquivos · ~30 KB · ~8k

**C17** - `DELETE /api/transactions/{id}` numa despesa de R$ 200,00 → `204`, some de `GET /api/transactions`, saldo +R$ 200,00
Proof: `TransactionDeletionTest.deletingExpenseRestoresBalance`

**C18** - Excluir receita de R$ 300,00 → saldo -R$ 300,00
Proof: `TransactionDeletionTest.deletingIncomeRemovesItFromBalance`

**C19** - Excluir transferência de R$ 500,00 → origem +R$ 500,00, destino -R$ 500,00
Proof: `TransactionDeletionTest.deletingTransferRestoresBothAccounts`

**C20** - Excluir compra de R$ 150,00 no cartão → fatura -R$ 150,00, limite disponível +R$ 150,00
Proof: `TransactionDeletionTest.deletingCardPurchaseRestoresInvoiceAndLimit`

**C21** - Excluir uma parcela de R$ 300,00 em 3x → as 3 somem, cada fatura -R$ 100,00, limite +R$ 300,00
Proof: `TransactionDeletionTest.deletingOneInstallmentRemovesTheWholePurchase`

**C22** - Excluir a ocorrência de fevereiro → estorno; novo processamento até 15/03/2026 não recria fevereiro e mantém março
Proof: `TransactionDeletionTest.deletedOccurrenceIsNotRecreated`

**C23** - Excluir a regra → estorno da regra; ocorrências geradas permanecem; processamento posterior não cria novas
Proof: `TransactionDeletionTest.deletingRuleStopsFutureOccurrences`

**C24** - `DELETE` com id inexistente → `400` `Transação não encontrada`
Proof: `TransactionDeletionTest.deletingUnknownTransactionIsRejected`

### S6 - Backup com a nova estrutura · 5 arquivos · ~40 KB · ~10k

**C25** - Exportar e importar em JSON (categoria personalizada, categoria arquivada, compra parcelada, regra com ocorrência excluída, orçamento) → categorias iguais; lançamentos e orçamentos apontam para elas; excluir uma parcela remove as 3; processar a recorrência não duplica nem recria a excluída
Proof: `BackupControllerTest.jsonRoundTripPreservesStructure`

**C26** - O mesmo cenário em CSV dá o mesmo resultado
Proof: `BackupControllerTest.csvRoundTripPreservesStructure`

**C27** - Backup com `categoryId` ausente de `categories` → `400` com `message` contendo `Backup inválido`
Proof: `BackupControllerTest.unknownCategoryIsRejected`

Todos os proofs rodam com: `JAVA_HOME="/c/Program Files/Java/jdk-21.0.11" mvn -B test -Dtest=<Classe>#<método>`; a suíte inteira com `mvn -B clean test`.

## Swept

- validation: C3, C5, C6, C11, C27
- failure modes: C24; existing - escritas em `@Transactional`
- idempotency and retry: C13
- authorization: n/a - PIN na entrega 3
- concurrency and ordering: n/a - JVM única; `UNIQUE(recurrence_source_id, "DATE")` impede a duplicata se dois disparos coincidirem
- data lifecycle: C4, C25, C26
- external-dependency failure: n/a - sem serviço externo
- state transitions: C4, C22, C23
- observability: n/a - logs padrão

## Handoff

Não há handoff: S1 a S6 somam aproximadamente 36k tokens de leitura, abaixo do orçamento de 150k. A fronteira natural, se fosse preciso, seria depois da S3 (dados) e antes da S4 (comportamento).
