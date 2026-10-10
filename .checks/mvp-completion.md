# MVP Completion

Sources:

- `.tasks/mvp-core.md` - critérios 6, 7, 8 e 12 e rotas do MVP
- `docs/product-spec.md` - regras de cartões, faturas, recorrência, parcelamento e backup manual
- `.github/copilot-instructions.md` - Java/Spring Boot, H2 local, idioma e modularidade

## Out of scope

- Autenticação e sincronização em nuvem - explicitamente fora do MVP.
- Reformulação completa das telas de cadastro - esta entrega fecha o domínio e mantém o dashboard existente.

## Landing

O domínio de faturas ficará no módulo `account`, reutilizando `CreditCard` e `Transaction`; a recorrência permanecerá no módulo `transaction`, com um scheduler fino chamando o serviço existente. O backup continuará em `backup`, mas usará substituição ordenada para preservar as relações JPA.

| One-way door | Literal shape | Alternative rejected |
| --- | --- | --- |
| Compra em cartão passa a referenciar cartão e fatura | `Transaction.creditCard` opcional e `CreditCardInvoice(billingMonth, amount, dueDate)` | guardar apenas texto do cartão, pois não permite consolidar faturas |
| Importação define substituição completa | apagar dependentes antes de persistir o snapshot importado | merge implícito por ID, pois mantém registros fora do backup e torna o resultado imprevisível |

- O arredondamento de parcelas usa centavos distribuídos na última parcela; não há nova dependência externa.

## Checks

### S1 - Compras e faturas de cartão · 6 arquivos · ~35 KB · ~9k

**C1** - Uma compra em cartão cria uma fatura no mês de vencimento e reduz o limite disponível pelo valor total.
Proof: `mvn -Dtest=CreditCardBillingTest test`

**C2** - Compras no mesmo cartão e mês são consolidadas na mesma fatura.
Proof: `mvn -Dtest=CreditCardBillingTest test`

### S2 - Recorrência automática · 4 arquivos · ~20 KB · ~5k

**C3** - O processamento automático gera a ocorrência mensal vencida sem alterar antecipadamente o saldo para datas futuras.
Proof: `mvn -Dtest=RecurringTransactionSchedulerTest test`

**C4** - Processar a mesma recorrência duas vezes não duplica a ocorrência.
Proof: `mvn -Dtest=RecurringTransactionSchedulerTest test`

### S3 - Parcelamento monetariamente exato · 2 arquivos · ~15 KB · ~4k

**C5** - R$ 100,00 em 3 parcelas gera R$ 33,33, R$ 33,33 e R$ 33,34, cuja soma é R$ 100,00.
Proof: `mvn -Dtest=InstallmentRoundingTest test`

### S4 - Backup substitutivo · 3 arquivos · ~25 KB · ~6k

**C6** - Importar um backup remove um registro antigo que não existe no arquivo importado.
Proof: `mvn -Dtest=BackupControllerTest test`

**C7** - Importar JSON e CSV preserva os relacionamentos entre perfil, conta e transação.
Proof: `mvn -Dtest=BackupControllerTest test`

**C8** - Arquivo inválido retorna erro de requisição com mensagem de backup inválido.
Proof: `mvn -Dtest=BackupControllerTest test`

## Swept

- validation: C1, C5 e C8
- failure modes: C8 e referências inexistentes no backup
- idempotency and retry: C4
- authorization: n/a - aplicação local sem autenticação por decisão do MVP
- concurrency and ordering: importação ordenada por dependência em C6/C7
- data lifecycle: C6 - importação substitui o estado local
- external-dependency failure: n/a - sem dependências externas
- state transitions: C1 e C3
- observability: n/a - logs padrão do Spring Boot

## Handoff

Não há handoff: as quatro slices permanecem dentro do orçamento estimado de aproximadamente 24k tokens de leitura e implementação.