# Entrega 4 - Lançamento rápido e extrato do mês

Sources:

- `.tasks/lancamento-rapido-extrato.md` - critérios 1 a 33, decisões e Unresolved 1 a 5
- Linear COD-13 - fonte original
- `docs/decisions.md` D5, D13, D17, D18, D19, D20, D21, D22

Profile: `light` (default; o projeto não declara `AGENTS.md`).

## Out of scope

- Editar e excluir lançamentos - entrega 7 (D19)
- Pagamento de fatura - entrega 5
- Persistir "quem sou eu" no servidor - D20 é por aparelho
- Retry automático de envio - Unresolved 3 da task

## Landing

Pacotes: `transaction` (filtro do extrato), `account` (validação de dia do cartão), `frontend/src/lib` (regras puras), `frontend/src/stores` (quem sou eu), `frontend/src/screens` (telas).

| One-way door | Literal shape | Alternative rejected |
| --- | --- | --- |
| Filtro do extrato no servidor por mês | `GET /api/transactions?month=AAAA-MM`, opcional; vira o intervalo `[dia 1, último dia]` sobre `date`. `profileId` existe na API (C2), mas o app filtra a pessoa no celular, sobre a lista do mês já carregada | filtrar o mês no app: cada celular baixaria todo o histórico a cada troca de mês |
| Tailwind v4 pelo plugin do Vite | `tailwindcss` e `@tailwindcss/vite` em `devDependencies`; `index.css` com `@import "tailwindcss"` | Tailwind v3 com PostCSS: config extra e versão antiga; CSS puro: D13 |
| Zustand com `persist` | store `useQuemSouEu` com chave `quem-sou-eu` no `localStorage`, guardando só o id | Context do React: some ao recarregar sem persistência manual; estado no servidor: D20 |
| Testes de lógica do frontend sem framework | `node --experimental-strip-types --test 'src/**/*.test.ts'`, via `npm test` | vitest: um runner e um transform novos para testar funções puras |
| Roteiros de tela versionados | `playwright-core` em `devDependencies`; `frontend/e2e/roteiros.mjs` roda com `npm run e2e` contra `127.0.0.1:8080`; o navegador vem de `CHROME_PATH` | `@playwright/test`: runner e config próprios; roteiros fora do repositório, como na entrega 3, sem prova versionada |

- Nada mais nesta mudança é difícil de reverter

## Checks

### S1 - Extrato filtra por mês e por pessoa (API) · ~3 arquivos · ~9 KB · ~2k

**C1** - `month` devolve só os lançamentos do mês, do mais recente para o mais antigo
Proof: `TransactionQueryApiTest#monthFilterReturnsOnlyThatMonthNewestFirst`

**C2** - `month` com `profileId` devolve só os lançamentos da pessoa naquele mês
Proof: `TransactionQueryApiTest#profileAndMonthFilter`

**C3** - `month` fora de `AAAA-MM` responde `400` com `message` = `Mês inválido, use o formato AAAA-MM`
Proof: `TransactionQueryApiTest#invalidMonthReturns400`

**C4** - Sem parâmetros, `GET /api/transactions` devolve todos os lançamentos
Proof: `TransactionQueryApiTest#withoutParamsReturnsEverything`

### S2 - Quem sou eu por aparelho · ~2 arquivos · ~3 KB · ~1k

**C6** - Escolher um perfil grava o id na chave `quem-sou-eu`
Proof: `npm test` - `frontend/src/stores/quemSouEu.test.ts#choosingStoresIdUnderKey`

**C7** - Com a chave gravada, a store reabre com o mesmo id
Proof: `frontend/src/stores/quemSouEu.test.ts#storeRestoresPersistedId`
Proof: `npm run e2e` - roteiro `quem-sou-eu: com escolha salva, recarregar abre direto no Resumo`

**C8** - Trocar substitui o id na mesma chave, sem criar outra
Proof: `frontend/src/stores/quemSouEu.test.ts#changingReplacesSameKey`
Proof: `npm run e2e` - roteiro `quem-sou-eu: trocar mostra a lista e a escolha nova substitui a anterior`

**C10** - Id salvo que não existe na lista de perfis não é aceito como selecionado
Proof: `frontend/src/stores/quemSouEu.test.ts#unknownIdIsNotSelected`
Proof: `npm run e2e` - roteiro `quem-sou-eu: id salvo inexistente mostra a tela de escolha`

**C5** - Aparelho sem escolha abre na tela "Quem é você?" com a lista de perfis
Proof: `npm run e2e` - roteiro `quem-sou-eu: sem escolha mostra a lista`

**C9** - Sem perfis, a tela "Quem é você?" mostra o botão para cadastrar perfil
Proof: `npm run e2e` - roteiro `quem-sou-eu: sem perfis mostra cadastro`

### S3 - Cadastro mínimo de perfis, contas e cartões · ~3 arquivos · ~6 KB · ~2k

**C11** - `POST /api/profiles` com `name` = `Maria` responde `201` e o perfil aparece na lista
Proof: `RegistrationApiTest#profileCreatedAppearsInList`

**C12** - `POST /api/accounts` com nome, tipo e saldo inicial responde `201` e a conta aparece na lista
Proof: `RegistrationApiTest#accountCreatedAppearsInList`

**C13** - `POST /api/credit-cards` com nome, limite e dias responde `201` e o cartão aparece na lista
Proof: `RegistrationApiTest#cardCreatedAppearsInList`

**C14** - Dia de fechamento ou vencimento fora de 1 a 31 responde `400` com `messages.closingDay` ou `messages.dueDay`
Proof: `RegistrationApiTest#cardDayOutsideOneToThirtyOneIsRejected`

### S4 - Lançamento rápido · ~4 arquivos · ~12 KB · ~3k

**C15** - Formulário abre com tipo `Despesa`, data de hoje e valor, categoria e "pagar com" vazios
Proof: `frontend/src/lib/quickEntry.test.ts#initialFormDefaults`

**C16** - Despesa de R$ 42,50 em Alimentação pagando com conta gera o payload com `type`, `amount`, `categoryId`, `accountId`, `date` e `profileId` esperados
Proof: `frontend/src/lib/quickEntry.test.ts#expensePayloadUsesAccount`
Proof: `npm run e2e` - roteiro `lancar: despesa vai para a API e limpa o valor`

**C17** - Valor vazio ou zero deixa `canSubmit` falso
Proof: `frontend/src/lib/quickEntry.test.ts#cannotSubmitWithoutPositiveAmount`

**C18** - Receita lista só contas em "pagar com"
Proof: `frontend/src/lib/quickEntry.test.ts#incomeOffersOnlyAccounts`

**C19** - Despesa lista contas e cartões juntos, com cartões marcados
Proof: `frontend/src/lib/quickEntry.test.ts#expenseOffersAccountsAndCards`

**C20** - Escolher cartão envia `creditCardId` e não `accountId`
Proof: `frontend/src/lib/quickEntry.test.ts#cardSelectionSendsCreditCardId`

**C21** - "Mais opções" mostra descrição, data, parcelas (só cartão, 1 a 24), recorrência e destino (só transferência)
Proof: `npm run e2e` - roteiro `lancar: mais opções muda os campos`

**C22** - Cartão com 3 parcelas cria 3 lançamentos em meses consecutivos
Proof: `QuickEntryApiTest#cardInstallmentsCreateThreeMonths`
Proof: `frontend/src/lib/quickEntry.test.ts#installmentPayloadCarriesCount`

**C23** - Descrição vazia usa o nome da categoria no payload
Proof: `frontend/src/lib/quickEntry.test.ts#blankDescriptionUsesCategoryName`

**C24** - Resposta `400` mostra a mensagem da API e mantém o formulário
Proof: `npm run e2e` - roteiro `lancar: erro da API fica na tela`

**C25** - Dois toques rápidos em Salvar geram um único lançamento
Proof: `npm run e2e` - roteiro `lancar: dois toques geram um lançamento`

### S5 - Extrato do mês · ~3 arquivos · ~6 KB · ~2k

**C26** - Totais são `Entradas` = soma das receitas, `Saídas` = soma das despesas e `Saldo` = entradas − saídas
Proof: `frontend/src/lib/finance.test.ts#summarizeSumsIncomeExpenseBalance`

**C27** - Filtro por pessoa restringe a lista e os totais
Proof: `frontend/src/lib/finance.test.ts#profileFilterAppliesToTotals`
Proof: `npm run e2e` - roteiro `extrato: filtro por pessoa restringe a lista`

**C28** - Navegação de mês e título em português
Proof: `frontend/src/lib/finance.test.ts#shiftMonthAcrossYearAndLabel`
Proof: `npm run e2e` - roteiro `extrato: mes anterior muda o titulo`

**C29** - Transferência aparece na lista e não entra nos totais
Proof: `frontend/src/lib/finance.test.ts#transfersAreListedButNotInTotals`

**C30** - Mês sem lançamentos mostra `Nenhum lançamento neste mês` e totais zero
Proof: `frontend/src/lib/finance.test.ts#emptyMonthTotalsAreZero`
Proof: `npm run e2e` - roteiro `extrato: mes vazio`

### S6 - Base de estilo e navegação no celular · ~3 arquivos · ~3 KB · ~1k

**C31** - Em 375px, Resumo, Lançar e Extrato não têm rolagem horizontal
Proof: `npm run e2e` - roteiro `celular: sem rolagem horizontal`

**C32** - Navegação inferior tem `Resumo`, `Lançar`, `Extrato`, com a ativa destacada
Proof: `npm run e2e` - roteiro `celular: abas`

**C33** - `App.css` não existe e `index.css` importa o Tailwind
Proof: `test ! -e frontend/src/App.css && grep -q '@import "tailwindcss"' frontend/src/index.css`

## States

Ver `.tasks/lancamento-rapido-extrato.md`, seção States: aparelho sem escolha, Resumo, id salvo inexistente.

## Coverage

| Set (size) | Member -> proof | Unproven |
| --- | --- | --- |
| tipos de lançamento para "pagar com" (3) | Despesa C19 · Receita C18 · Transferência origem só conta (Unresolved 1 da task) | - |
| dias de cartão (2) | fechamento C14 · vencimento C14 | - |
| formas de pagamento no envio (2) | `accountId` C16 · `creditCardId` C20 | - |
| meses do extrato (2 lados do ano) | dezembro→janeiro C28 · janeiro→dezembro C28 | - |
| estados de tela de carregar (4) | sem perfil C9 · sem escolha C5 · erro C24 · vazio C30 | loading de tela: Unresolved 2 da task |

- Claims naming a status code or response shape: C3, C11, C12, C13, C14, C22 - each has a proof that crosses MockMvc (`TransactionQueryApiTest`, `RegistrationApiTest`, `QuickEntryApiTest`)
- Claims about screens (C5, C7, C8, C9, C10, C16 envio, C21, C24, C25, C27, C28 título, C30 vazio, C31, C32) are proven by `npm run e2e` against a running backend, not by unit tests. `npm run e2e` precisa de um servidor descartável e de `E2E_PIN` e `CHROME_PATH`; C9 só roda com o banco sem perfis, então o roteiro deve começar num servidor vazio

## Handoff

Single batch. Estimated reading: ~40 KB total (≈ 10k tokens), far under 150k. No handoff.

## Verificação

Feita na própria sessão de build (sem Verifier separado, como na entrega 3). Todas as provas foram executadas nesta sessão:

- `npm test`: 18/18 (lógica pura e store)
- `QuickEntryApiTest`, `TransactionQueryApiTest`, `RegistrationApiTest`: 9/9; suíte de backend completa 73/73 (`mvn package`)
- `npm run e2e` contra servidor descartável, banco vazio: 17/17 roteiros, incluindo C9
- C33: `test ! -e frontend/src/App.css && grep -q '@import "tailwindcss"' frontend/src/index.css` passou
- `npm run build` e `npm run lint` passaram

**Gaps declarados**

- C29 (transferências fora dos totais) está provado só pela lógica pura; não há roteiro de tela para isso.
- C26 (totais) também só pela lógica pura.
- Sem Verifier independente, como na entrega 3.

**Desvios registrados**

- O filtro por pessoa é feito no celular sobre a lista do mês. A API aceita `profileId` (C2), mas o app não usa esse parâmetro.
- Quando a validação nativa do HTML bloqueava o envio do cadastro de cartão (`min`/`max`), desliguei a validação nativa nos três formulários de cadastro (`noValidate`). A regra fica no servidor, que devolve a mensagem.
