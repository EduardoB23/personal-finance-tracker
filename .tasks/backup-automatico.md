# Entrega 6 - Backup automático diário (OneDrive)

> Build this with **tlc-implement**.
> Every criterion below becomes a check with a proof, referenced by its number. Nothing under
> `Unresolved` gets settled while building.

## Intent

Hoje o backup só existe quando alguém clica em exportar. Se o PC de casa falha ou um arquivo some, os
lançamentos de meses inteiros se perdem, e ninguém percebe até precisar. Quem paga é a família, que
guarda dados financeiros reais num único computador.

Com esta entrega, o servidor grava todo dia um backup JSON numa pasta configurável (padrão: OneDrive) e
mantém os últimos 30. Ele também faz o backup ao iniciar, se o de hoje não existir. Se uma tentativa
falha, o app avisa no Resumo. A tela de Configurações ganha exportar (JSON e CSV) e importar, com
confirmação, como a COD-8 prometia.

0 abertos bloqueiam · 2 abertos · 15 critérios em 3 fatias · 1 porta sem retorno

## Criteria

### S1 - Backup automático (servidor)

1. Dado que não existe backup de hoje na pasta, quando o servidor inicia, então grava `finance-backup-AAAA-MM-DD.json` nela.
2. Dado que o backup de hoje já existe, quando o servidor inicia, então não sobrescreve o arquivo.
3. Quando o job diário roda, então grava o backup do dia e está agendado para `0 10 0 * * *`.
4. Dada a pasta com 31 backups antigos, quando um backup novo é gravado, então restam os 30 mais recentes.
5. O arquivo gerado é um backup JSON que a importação lê de volta.
6. Se a pasta não pode ser criada ou gravada, então o status guarda a mensagem de erro e o servidor continua no ar.
7. Depois de uma falha, quando o backup seguinte funciona, então o erro some do status.
8. `GET /api/backup/automatic-status` responde os campos `lastSuccessAt`, `lastFile`, `lastError` e `lastErrorAt`.
9. Sem `FINANCE_BACKUP_DIR`, a pasta padrão é `${user.home}/OneDrive`.

### S2 - Tela de Configurações

10. Quando o usuário toca em "Exportar JSON", então baixa `finance-backup.json`.
11. Quando o usuário toca em "Exportar CSV", então baixa `finance-backup.csv`.
12. Quando o usuário escolhe um arquivo para importar, então aparece `Importar substitui todos os dados atuais. Continuar?` e nada é enviado antes de confirmar.
13. Quando a importação é confirmada, então mostra `Backup importado com sucesso` e o app recarrega os dados.

### S3 - Aviso no Resumo e celular

14. Quando o último backup automático falhou, então o Resumo mostra `Backup automático falhou: <erro>` e a data do último backup que deu certo; sem falha, o aviso não aparece.
15. Em 375px, a tela de Configurações não tem rolagem horizontal.

## Out of scope

- Importar um backup automático escolhido na pasta, pela tela - a importação usa o arquivo escolhido no seletor
- Histórico de falhas - o status guarda só a última
- Sincronização com o OneDrive - a pasta é sincronizada pelo próprio OneDrive

## Observable

| Surface | Decision | Landing |
| --- | --- | --- |
| aviso no Resumo | empty state | 14 (sem falha, nenhum aviso) |
| aviso no Resumo | error state | 14 |
| tela Configurações | loading state | n/a - as ações são locais ou enviam um arquivo |
| tela Configurações | destructive action confirms | 12 |
| API `GET /api/backup/automatic-status` | error shape | 8 (campo `lastError`) |
| API `GET /api/backup/export` e `POST /api/backup/import` | error shape | já existente, COD-8 |

## Swept

- validation: 5 (o arquivo gerado passa na importação)
- failure modes: 6, 7
- idempotency and retry: 2 (não sobrescreve no início), 3
- authorization: existing - guard de PIN de `/api/**`
- concurrency and ordering: `runBackup` é `synchronized`; sem prova de concorrência
- data lifecycle: 4 (retenção de 30)
- external-dependency failure: 6 (pasta indisponível vira aviso)
- state transitions: 6 e 7
- observability: 6 (log `WARN` com a pasta e a mensagem)

## Impact

| Front | What changes |
|---|---|
| domain | new term: `Backup automático` - arquivo diário em `finance.backup-dir`, vive em `backup` |
| config | `application.yml`: `finance.backup-dir: ${FINANCE_BACKUP_DIR:${user.home}/OneDrive}` |
| UI | Resumo ganha o aviso e o botão Configurações; Configurações é uma tela nova |
| tests | `src/test/resources/application.properties` aponta o backup de teste para `target/test-backups` |

## Decided

| Decision | Shape | Alternative rejected |
| --- | --- | --- |
| Status do backup em memória | `AutomaticBackupService.Status` (último sucesso, arquivo, último erro, data do erro) | status em tabela: uma linha a mais no banco, e o aviso sobreviveria a reinício sem ter o que dizer |

## Sources

- Linear COD-15, *Entrega 6 - Backup automático diário (OneDrive)*, 10/10/2026 - fonte principal
- `docs/decisions.md` D23 (H2 é a fonte da verdade), D24 (backup diário, últimos 30, pasta configurável), D25 (roda na inicialização; falha vira aviso)
- Linear COD-8 - a tela de Configurações com exportar e importar, prometida e ainda sem tela

## Unresolved

| # | Kind | Question | Until answered |
| --- | --- | --- | --- |
| 1 | open | Horário do job diário. Default: 00:10, cinco minutos depois do job de recorrência (00:05) | Critério 3 usa 00:10 |
| 2 | open | Pasta padrão no Windows. Default: `${user.home}/OneDrive`, que é a pasta do OneDrive quando o aplicativo está instalado com a conta pessoal | Critério 9 prova a configuração, não a pasta real no PC |
