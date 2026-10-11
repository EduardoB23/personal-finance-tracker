# Entrega 7 (parcial) - Planilha, orçamento e metas

> Build this with **tlc-implement**. Nothing under `Unresolved` is decided while building.

## Intent

Os itens da COD-16 que não dependem de decisão de produto ainda não existem: a família não tem como
ler os dados numa planilha, nem criar um orçamento ou uma meta pelo app (só ver). Quem paga é quem
precisa conferir o mês no computador ou planejar uma compra. Com esta entrega, a família exporta uma
planilha de leitura, cria orçamentos e metas e faz aportes em metas pelo celular.

Ficam de fora desta entrega, por decisão pendente ou por roadmap: edição de lançamentos com
recálculo (decisão 3), metas por família (decisão 1), parcelamento em conta por mês (decisão 2) e
projeções (fase 3 do roadmap).

2 abertos (as decisões 1 a 3 da COD-16) · 14 critérios em 3 fatias · 1 dependência nova

## Criteria

### S1 - Planilha de leitura (.xlsx)

1. `GET /api/backup/export?format=xlsx` responde `200` com `Content-Type` `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` e arquivo `finance-backup.xlsx`.
2. A planilha tem uma aba por entidade: `Perfis`, `Categorias`, `Contas`, `Cartoes`, `Faturas`, `Pagamentos`, `Orcamentos`, `Metas`, `Lancamentos`, `Excecoes`.
3. Cada aba tem cabeçalho com os nomes das colunas do backup JSON e uma linha por registro.
4. Valores monetários são células numéricas, não texto.
5. `POST /api/backup/import` com um `.xlsx` responde `400` com `message` = `Planilha não é aceita para importação; use o JSON ou o CSV`.
6. `format` desconhecido responde `400` mencionando `XLSX`.
7. Em Configurações, "Exportar Excel (leitura)" baixa `finance-backup.xlsx`.

### S2 - Orçamentos pelo celular

8. Em Orçamentos, criar um orçamento com categoria e limite mostra `Orçamento criado` e o orçamento aparece na lista do mês.
9. Criar um segundo orçamento para a mesma categoria e mês mostra `Já existe orçamento para esta categoria neste mês`.
10. Em 375px, a tela de Orçamentos não tem rolagem horizontal.

### S3 - Metas pelo celular

11. Em Metas, criar uma meta com título, valor alvo e perfil mostra `Meta criada`.
12. Um aporte de R$ 250,00 numa meta de R$ 1.000,00 mostra `Aporte registrado` e `25% da meta`.
13. Em 375px, a tela de Metas não tem rolagem horizontal.
14. Aporte de zero ou vazio não é enviado (botão e lógica pura).

## Out of scope

- Edição completa de lançamentos (decisão 3 da COD-16)
- Metas da família (decisão 1)
- Parcelamento em conta por mês (decisão 2)
- Projeções (fase 3 do roadmap)
- Importar planilha - a planilha é só de leitura (D23)

## Decided

| Decision | Shape | Alternative rejected |
| --- | --- | --- |
| Apache POI para gerar `.xlsx` | dependência `org.apache.poi:poi-ooxml:5.2.5` no `pom.xml`; uma aba por lista do `BackupExport`, colunas pelos componentes do record | escrever o zip do `.xlsx` à mão: mais código para manter sem ganho de funcionalidade |
| Planilha não é formato de importação | importação rejeita `.xlsx` com 400 | importar planilha: duas fontes de verdade, contra D23 |

## Unresolved

| # | Kind | Question | Until answered |
| --- | --- | --- | --- |
| 1 | open | Metas são da família ou de um perfil? (COD-11) | Criação pede perfil, como hoje |
| 2 | open | Parcelamento em conta debitando por mês (hoje debita tudo na criação) | Fora desta entrega |
