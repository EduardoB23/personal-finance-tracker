# Entrega 6 - Backup automático diário

Sources:

- `.tasks/backup-automatico.md` - critérios 1 a 15
- Linear COD-15 e COD-8
- `docs/decisions.md` D23, D24, D25

Profile: `light` (default).

## Out of scope

- Importar um backup automático escolhido na pasta, pela tela
- Histórico de falhas
- Sincronização com o OneDrive

## Landing

Pacote `backup`: `AutomaticBackupService` (escrita, retenção, status), `AutomaticBackupController` (status). Tela `frontend/src/screens/Configuracoes.tsx`.

| One-way door | Literal shape | Alternative rejected |
| --- | --- | --- |
| Status em memória | `Status(lastSuccessAt, lastFile, lastError, lastErrorAt)` no serviço | status em tabela: aviso sobreviveria a reinício sem nova informação |

- Nada mais nesta mudança é difícil de reverter

## Checks

### S1 - Backup automático (servidor)

**C1** - Servidor sobe sem backup de hoje e grava `finance-backup-AAAA-MM-DD.json`
Proof: `AutomaticBackupServiceTest#startupWritesTodaysBackup`

**C2** - Servidor sobe com backup de hoje e não sobrescreve
Proof: `AutomaticBackupServiceTest#startupSkipsWhenTodaysBackupExists`

**C3** - Job diário grava o arquivo e está agendado para `0 10 0 * * *`
Proof: `AutomaticBackupServiceTest#dailyJobWritesFileAtZeroTenAndIsScheduled`

**C4** - Com 31 antigos, restam os 30 mais recentes
Proof: `AutomaticBackupServiceTest#keepsLastThirtyFiles`

**C5** - O arquivo gerado volta pelo leitor de backup
Proof: `AutomaticBackupServiceTest#generatedFileRoundTrips`

**C6** - Pasta inválida grava erro no status e não derruba o servidor
Proof: `AutomaticBackupServiceTest#missingFolderRecordsErrorAndDoesNotCrash`

**C7** - Backup seguinte que funciona limpa o erro
Proof: `AutomaticBackupServiceTest#successClearsPreviousError`

**C8** - Endpoint de status responde os campos
Proof: `AutomaticBackupStatusApiTest#statusEndpointReportsFields`

**C9** - Pasta padrão é `${user.home}/OneDrive` sem `FINANCE_BACKUP_DIR`
Proof: parcial. O teste `defaultFolderIsOneDrive` só prova que os testes apontam para `target/`. A configuração de produção foi conferida por leitura de `application.yml`, não por execução.

### S2 - Tela de Configurações

**C10** - Exportar JSON baixa `finance-backup.json`
Proof: `npm run e2e` - roteiro `backup: exportar JSON baixa finance-backup.json`

**C11** - Exportar CSV baixa `finance-backup.csv`
Proof: `npm run e2e` - roteiro `backup: exportar CSV baixa finance-backup.csv`

**C12** - Importar pede confirmação antes de enviar
Proof: `npm run e2e` - roteiro `backup: importar pede confirmação antes de enviar`

**C13** - Confirmar importação mostra sucesso
Proof: `npm run e2e` - roteiro `backup: confirmar importação mostra sucesso`

### S3 - Aviso no Resumo e celular

**C14** - Falha mostra o aviso no Resumo; sem falha, nenhum aviso
Proof: `npm run e2e` - roteiro `backup: nenhum aviso de falha no Resumo` (caso sem falha)
Proof: teste manual com `FINANCE_BACKUP_DIR` apontando para um arquivo: o aviso `Backup automático falhou: ...` apareceu no Resumo. Não é roteiro commitado.

**C15** - Configurações sem rolagem horizontal em 375px
Proof: `npm run e2e` - roteiro `backup: sem rolagem horizontal em Configurações`

## Coverage

| Set (size) | Member -> proof | Unproven |
| --- | --- | --- |
| resultado do backup (2) | sucesso C1 · falha C6 | - |
| ações de tela (4) | exportar JSON C10 · CSV C11 · importar com confirmação C12 · importação C13 | - |

## Verificação

Feita na própria sessão de build, sem Verifier separado. Suíte de backend completa: 100/100 (`mvn package`). Roteiros de tela: 32/32 num servidor limpo. Teste manual de falha: aviso confirmado.

**Gaps declarados**

- C9: o padrão de produção (`${user.home}/OneDrive`) não foi executado num PC Windows; a pasta real do OneDrive depende da instalação.
- C14: o caminho de falha tem prova manual, não roteiro versionado.
- Concorrência de `runBackup` não tem prova.
- O status vive em memória: um reinício apaga o último erro até o próximo backup, que roda na inicialização.
