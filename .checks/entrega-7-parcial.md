# Entrega 7 (parcial)

Sources: `.tasks/entrega-7-parcial.md`; Linear COD-16; D23.

Profile: `light`.

## Checks

**C1** - Export xlsx responde 200 com o tipo e o arquivo certos
Proof: `BackupXlsxTest#exportHasOneSheetPerEntityWithNumericAmounts` (abas e cabeçalho) + roteiro `backup: exportar Excel baixa finance-backup.xlsx`

**C2 e C3** - Uma aba por entidade, cabeçalho com nomes de colunas, uma linha por registro
Proof: `BackupXlsxTest#exportHasOneSheetPerEntityWithNumericAmounts`

**C4** - Valores monetários são numéricos
Proof: `BackupXlsxTest#exportHasOneSheetPerEntityWithNumericAmounts` (`getNumericCellValue` na aba Contas)

**C5** - Importar `.xlsx` responde 400 com a mensagem certa
Proof: `BackupXlsxTest#xlsxIsRejectedForImport`

**C6** - Formato desconhecido menciona XLSX
Proof: `BackupXlsxTest#unknownFormatMentionsXlsx`

**C7** - Botão Exportar Excel baixa o arquivo
Proof: `npm run e2e` - roteiro `backup: exportar Excel baixa finance-backup.xlsx`

**C8** - Criar orçamento mostra confirmação e aparece na lista
Proof: `npm run e2e` - roteiro `orcamentos: criar mostra o orçamento e repete o erro de duplicado`

**C9** - Duplicado mostra a mensagem da API
Proof: mesmo roteiro (segunda parte)

**C10** - Orçamentos sem rolagem horizontal em 375px
Proof: mesmo roteiro (última linha)

**C11** - Criar meta mostra confirmação
Proof: `npm run e2e` - roteiro `metas: criar meta e aportar mostra o progresso`

**C12** - Aporte mostra confirmação e o percentual
Proof: mesmo roteiro

**C13** - Metas sem rolagem horizontal em 375px
Proof: mesmo roteiro

**C14** - Aporte zero ou vazio não é enviado
Proof: `frontend/src/lib/budgetsGoals.test.ts#contributionMustBePositive`

## Verificação

Suíte de backend completa: 103/103 (`mvn package`). Lógica do frontend: 26/26. Roteiros de tela: 35/35 num servidor limpo.

Gaps declarados:
- Sem Verifier independente.
- Abrir o `.xlsx` no Excel real não foi testado; a leitura foi feita com o próprio POI.
- A tela de Orçamentos e a de Metas foram conferidas só pelo roteiro, não por estado vazio com dados reais de carregamento lento.
