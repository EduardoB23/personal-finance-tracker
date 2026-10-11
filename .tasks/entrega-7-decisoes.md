# Entrega 7 - Decisões da COD-16 e edição de lançamentos

## Decisões da família

- Metas são da família, como contas, cartões e orçamentos. O perfil sai da meta (migração `V5`).
- Parcelamento em conta continua debitando tudo na criação. Confirmado.
- Edição de lançamentos entra, com estorno e reaplicação na mesma transação.

## Critérios da edição (padrões adotados nesta versão)

1. Edita-se descrição, valor, data, categoria e "pagar com". O tipo não muda.
2. Parcelas e recorrências não são editáveis. A tela mostra a nota; a API responde `400`.
3. Mudar a data de uma compra no cartão move a compra para a fatura do novo mês; o estorno e a reaplicação
   usam a mesma regra de exclusão e de compra (inclusive a recusa quando a fatura já tem pagamento).
4. Quem lançou (`profileId`) não muda na edição.

## Em aberto (para decidir em outra entrega, se a família quiser)

- Editar parcelas (uma parcela, o grupo todo ou dali em diante).
- Editar recorrências (só uma ocorrência ou a regra).
- Trocar o tipo de um lançamento.
