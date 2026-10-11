# Entrega 7 - decisões e edição de lançamentos

Sources: `.tasks/entrega-7-decisoes.md`; Linear COD-16; D19.

Profile: `light`.

## Checks

**C1** - Meta criada sem perfil (família) responde 201 e não traz `profileId`
Proof: `GoalFamilyApiTest#goalIsFamilyWithoutProfile`

**C2** - Aporte continua limitado ao valor-alvo
Proof: `GoalFamilyApiTest#contributionStillCapsAtTarget`

**C3** - Editar o valor ajusta o saldo da conta
Proof: `TransactionEditApiTest#editAmountAdjustsAccountBalance`

**C4** - Mudar a data de uma compra no cartão move a fatura, sem mexer no limite
Proof: `TransactionEditApiTest#editDateMovesCardPurchaseToAnotherInvoice`

**C5** - Parcelas não são editáveis (400)
Proof: `TransactionEditApiTest#editingInstallmentsIsRefused`

**C6** - Tipo não muda (400 com mensagem)
Proof: `TransactionEditApiTest#changingTypeIsRefused`

**C7** - Id inexistente responde 400
Proof: `TransactionEditApiTest#unknownTransactionIsRejected`

**C8** - Regras de edição na tela (simples editável, parcela e recorrência não)
Proof: `frontend/src/lib/transactionEdit.test.ts#onlySimpleTransactionsAreEditable`

**C9** - Formulário nasce do lançamento e mantém quem lançou
Proof: `frontend/src/lib/transactionEdit.test.ts#formCarriesCardOrAccountAsPaymentSource`, `#editBodyKeepsWhoRecordedIt`

**C10** - Salvar edição mostra "Lançamento atualizado" e aparece na lista
Proof: `npm run e2e` - roteiro `extrato: editar um lançamento simples salva e mostra a atualização`

**C11** - Parcela mostra a nota de não editável
Proof: `npm run e2e` - roteiro `extrato: parcelas mostram que não podem ser editadas`

**C12** - Meta sem perfil na tela (criar e aportar)
Proof: `npm run e2e` - roteiro `metas: criar meta e aportar mostra o progresso`

## Verificação

Suíte de backend completa: 110/110. Lógica do frontend: 29/29. Roteiros de tela: 37/37 num servidor limpo.

Gaps:
- Sem Verifier independente.
- Editar parcela e recorrência ficou fora, por decisão desta entrega (ver `.tasks/entrega-7-decisoes.md`).
- Edição de compra de cartão em fatura já paga segue a regra de exclusão (pode ser recusada); não há roteiro específico para isso.
