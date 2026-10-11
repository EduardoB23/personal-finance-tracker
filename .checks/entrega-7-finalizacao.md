# Entrega 7 - finalização (parcelas e recorrências)

Sources: Linear COD-16 (reaberta); `.checks/entrega-7-decisoes.md`.

Profile: `light`.

## Decisões adotadas (sem resposta explícita; registradas para revisão)

- Editar uma parcela muda só aquela parcela (valor, data, descrição, categoria, conta/cartão). O grupo não muda como um todo.
- Editar uma ocorrência recorrente muda só aquela ocorrência. Se a data muda, o dia original vira exceção e não é recriado.
- A regra recorrente não é editada por aqui.
- Projeções de saldo ficam fora (roadmap fase 3).

## Checks

**C1** - Parcela editada muda só ela; a irmã mantém o valor
Proof: `TransactionEditApiTest#editingOneInstallmentChangesOnlyThatInstallment`

**C2** - Regra recorrente não é editável (400 com mensagem)
Proof: `TransactionEditApiTest#editingRecurringRuleIsRefused`

**C3** - Ocorrência com data trocada não é recriada no dia original
Proof: `TransactionEditApiTest#movingRecurringOccurrenceRecordsExceptionForOriginalDay`

**C4** - Tela: parcela tem botão Editar
Proof: `npm run e2e` - roteiro `extrato: parcelas também podem ser editadas uma a uma`

**C5** - Tela: regra recorrente mostra a nota
Proof: `frontend/src/lib/transactionEdit.test.ts#onlyTheRecurringRuleIsNotEditable` (regra da nota; sem roteiro de tela para a regra)

## Verificação

Backend: 112/112 (`mvn package`). Frontend: 29/29 de lógica, lint e build. Roteiros de tela: 37/37 num servidor limpo (após rebuild do bundle).

Gaps:
- Sem Verifier independente.
- Edição de ocorrência recorrente na tela não tem roteiro; a prova é da API.
- Edição de parcela altera o valor de uma parcela, e o total do parcelamento pode deixar de bater com o total comprado. Não há validação de soma (decisão a confirmar).
