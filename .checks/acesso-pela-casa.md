# Entrega 3 - Acesso pela casa

Sources:

- `.tasks/acesso-pela-casa.md` - critérios 1 a 30, decisões e respostas D33 (5 tentativas, 15 min), sessão em tabela H2, início por logon
- Linear COD-12 - fonte original dos critérios
- `docs/decisions.md` D3, D4, D5, D6, D7, D33

Profile: `light` (default; o projeto não declara `AGENTS.md`).

## Out of scope

- Logout - não está na issue
- Login individual (D8) e HTTPS - a issue exclui
- Executar os scripts do Windows nesta máquina - não há PowerShell aqui; quem executa é o usuário
- Limpeza de linhas expiradas da tabela `sessao` - volume de poucos aparelhos

## Landing

Módulo novo `com.finance.tracker.access`. Reusa o formato de erro de `GlobalExceptionHandler` (`timestamp`, `status`, `error`, `message`) e o de validação (`messages`). Sem Spring Security: um filtro único em `/api/**`.

| One-way door | Literal shape | Alternative rejected |
| --- | --- | --- |
| Dependência `spring-security-crypto` | BCrypt via `BCryptPasswordEncoder`, versão gerida pelo parent Boot | Spring Security completo - cadeia de filtros e login que o projeto não tem; `jbcrypt` - sem gerenciamento de versão pelo parent |
| Hash do PIN em `FINANCE_PIN_HASH` | `finance.pin-hash: ${FINANCE_PIN_HASH:}`; subida falha se vazio ou sem prefixo `$2` | PIN em texto ou SHA-256 sem salt - um PIN de 4 dígitos sai por força bruta |
| Migração `V3__sessao.sql` | `sessao (token_hash VARCHAR(64) PK, criada_em TIMESTAMP NOT NULL, expira_em TIMESTAMP NOT NULL)`; token_hash é SHA-256 hex do cookie | sessão em memória - some a cada reinício; contradiz os 90 dias |
| Cookie `FINANCE_SESSION` | `HttpOnly; SameSite=Strict; Path=/; Max-Age=7776000`, sem `Secure` | `Secure` - navegador não envia o cookie em HTTP |
| Contrato `POST /api/auth/pin` | entrada `{"pin": string}`; `204` com cookie; `401 {message: "PIN incorreto"}`; `429 {message: "Muitas tentativas. Tente novamente em 15 minutos"}`; `400 {messages.pin}` | token no corpo JSON - a tela teria de guardar e reenviar o token, contra o D6 |
| Limite de tentativas em memória por IP | `synchronized` sobre o mapa; check e contagem na mesma seção crítica | contagem fora do lock - 10 requisições simultâneas passam todas pelo check antes de qualquer incremento |
| Início por logon com diretório de trabalho do projeto | `-AtLogOn -User <usuário>` e `-WorkingDirectory <raiz do projeto>` | gatilho de boot - exige administrador; sem `WorkingDirectory`, `./data/finance` aponta para outro banco |
| Frontend no `.jar` via `static/` | `pom.xml` copia `frontend/dist` para `static/`; `finalName` = `personal-finance-tracker` | `frontend-maven-plugin` - baixa Node a cada build |

- Nada mais nesta mudança é difícil de reverter

## Checks

### S1 - Acesso com PIN na API · ~9 arquivos · ~22 KB · ~6k

**C1** - Subida sem `FINANCE_PIN_HASH` falha com a mensagem de orientação
Proof: `PinConfigurationTest#missingHashFailsStartupWithInstructions`

**C1b** - Subida sem a variável encerra o processo com código diferente de 0
Proof: `mvn -q package -DskipTests && env -u FINANCE_PIN_HASH java -jar target/personal-finance-tracker.jar; test $? -ne 0`

**C1c** - Hash sem prefixo `$2` falha a subida
Proof: `PinConfigurationTest#hashWithoutBcryptPrefixFailsStartup`

**C2** - `--gerar-hash-pin=<pin>` imprime um hash BCrypt e sai com código 0 sem subir o servidor
Proof: `PinHashCliTest#printsBcryptHashAndReturnsZero`

**C2b** - Com o comando, a porta 8080 não abre
Proof: `mvn -q package -DskipTests && (java -jar target/personal-finance-tracker.jar --gerar-hash-pin=1234 >/dev/null; ! (exec 3<>/dev/tcp/127.0.0.1/8080) 2>/dev/null)`

**C3** - Sem sessão, qualquer `/api/**` fora de `/api/auth/**` responde `401` com `message` = `PIN necessário`
Proof: `AccessPinApiTest#apiWithoutSessionReturns401WithPinMessage`

**C4** - PIN correto responde `204` com cookie `FINANCE_SESSION` `HttpOnly`, `SameSite=Strict`, `Max-Age=7776000`, sem `Secure`
Proof: `AccessPinApiTest#correctPinReturns204AndSessionCookie`

**C5** - Com o cookie de C4, `GET /api/profiles` responde `200`
Proof: `AccessPinApiTest#correctPinReturns204AndSessionCookie` (mesma asserção, segunda chamada)

**C6** - PIN errado responde `401` com `message` = `PIN incorreto` e sem cookie
Proof: `AccessPinApiTest#wrongPinReturns401WithoutCookie`

**C7** - `pin` vazio ou ausente responde `400` com `messages.pin`
Proof: `AccessPinApiTest#blankPinReturns400WithValidationShape`

**C8** - A 5ª tentativa errada seguida do IP responde `401`, e as seguintes do mesmo IP respondem `429` com a mensagem de bloqueio, mesmo com o PIN correto
Proof: `AccessPinApiTest#fifthWrongAttemptBlocksIpEvenWithCorrectPin`

**C9** - Tentativas durante o bloqueio não reiniciam o relógio; a primeira depois de 15 minutos com PIN correto responde `204`
Proof: `PinAttemptLimiterTest#attemptsWhileBlockedDoNotExtendBlock`
Proof: `PinAttemptLimiterTest#blockEndsFifteenMinutesAfterFifthWrong`

**C10** - Bloqueio de um IP não afeta outro IP
Proof: `AccessPinApiTest#blockIsPerIp`

**C11** - Acerto antes da 5ª errada zera a contagem: 4 erros, 1 acerto, 4 erros não bloqueiam
Proof: `PinAttemptLimiterTest#correctPinResetsCount`

**C12** - 10 tentativas erradas simultâneas do mesmo IP produzem no máximo 5 `401`
Proof: `PinAttemptLimiterTest#concurrentWrongAttemptsProduceAtMostFiveUnauthorized`

**C13** - Nenhuma linha de log contém o PIN enviado nem o valor de `FINANCE_PIN_HASH`
Proof: `PinLogTest#neverLogsPinOrHash`

**C14** - Sessão gravada em `sessao` continua válida depois de o serviço ser recriado
Proof: `AccessSessionServiceTest#sessionSurvivesServiceRecreation`
(Nota: prova a persistência pela tabela, não por um reinício da JVM. O reinício real é verificado manualmente no Verify.)

**C15** - Sessão deixa de valer 90 dias depois da criação, no servidor
Proof: `AccessSessionServiceTest#sessionExpiresAfterNinetyDays`

**C16** - Cookie alterado ou sem sessão correspondente responde `401` com `message` = `PIN necessário`
Proof: `AccessPinApiTest#tamperedCookieReturns401`

### S2 - Tela de PIN no frontend · ~1 arquivo · ~6 KB · ~2k

**C17** - Qualquer `401` de `/api/**` mostra a tela de PIN, com campo e botão Entrar, no lugar do dashboard
Proof: `npm run build` (em `frontend/`) e `npm run lint`; comportamento: roteiro Playwright no Verify, não commitado

**C18** - `204` do PIN some com a tela e carrega o dashboard sem recarregar a página
Proof: roteiro Playwright no Verify, não commitado

**C19** - `401` do PIN mostra `PIN incorreto` abaixo do campo
Proof: roteiro Playwright no Verify, não commitado

**C20** - `429` mostra o `message` recebido da API
Proof: roteiro Playwright no Verify, não commitado

### S3 - H2 console desligado e frontend no `.jar` · ~3 arquivos · ~3 KB · ~1k

**C21** - `GET /h2-console` responde `404`
Proof: `H2ConsoleDisabledTest#h2ConsoleIsNotServed`

**C22** - `.jar` gerado serve `GET /` com `200` `text/html` e `/assets/**` com `200`, sem sessão
Proof: `(cd frontend && npm run build) && mvn -q package -DskipTests && (java -jar target/personal-finance-tracker.jar &) && sleep 25 && curl -sf -o /dev/null -w '%{http_code} %{content_type}' http://127.0.0.1:8080/` esperando `200 text/html`, e `/assets/` do HTML respondendo `200`

**C23** - Com o `.jar` no ar e sem sessão, `GET /api/profiles` responde `401` enquanto `GET /` responde `200`
Proof: o comando de C22 seguido de `curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/profiles` esperando `401`

### S4 - Início com o Windows · ~3 arquivos · ~4 KB · ~1k

**C24** - O instalador registra a tarefa `Personal Finance Tracker` com `-AtLogOn`, `-User`, `-WorkingDirectory` e sai com 0
Proof: `WindowsScriptsTest#installerRegistersLogonTaskWithWorkingDirectory`

**C25** - Segunda execução substitui a tarefa: `Register-ScheduledTask -Force`, uma tarefa com esse nome
Proof: `WindowsScriptsTest#installerIsIdempotent`

**C26** - O remover apaga a tarefa e sai com 0
Proof: `WindowsScriptsTest#removerUnregistersTask`

**C27** - Sem JDK 21 (`JAVA_HOME` ausente ou sem `bin\java.exe`), o instalador sai com código diferente de 0 e não chama `Register-ScheduledTask`
Proof: `WindowsScriptsTest#installerFailsWithoutJdkBeforeRegistering`

### S5 - Regressão e documentação · ~4 arquivos · ~5 KB · ~1k

**C28** - `mvn test` sem `FINANCE_PIN_HASH` termina com 0 falhas
Proof: `env -u FINANCE_PIN_HASH mvn -B test` - exit 0

**C29** - `mvn test` não altera `data/finance.mv.db`
Proof: `TestDatabaseIsolationTest` (existente) e o comando: `before=$(test -e data/finance.mv.db && sha256sum data/finance.mv.db); mvn -q -B test; after=$(test -e data/finance.mv.db && sha256sum data/finance.mv.db); test "$before" = "$after"`

**C30** - O guia em `docs/acesso-pela-casa.md` tem os comandos de hash, `setx` e os dois scripts
Proof: `grep -q -- '--gerar-hash-pin' docs/acesso-pela-casa.md && grep -q 'setx FINANCE_PIN_HASH' docs/acesso-pela-casa.md && grep -q 'instalar-inicio-automatico.ps1' docs/acesso-pela-casa.md && grep -q 'remover-inicio-automatico.ps1' docs/acesso-pela-casa.md`

## States

Ver `.tasks/acesso-pela-casa.md`, seção States: `Livre`, `Contando`, `Bloqueado`, com as arestas 8, 9 e 11.

## Swept

- validation: C7 (400 com `messages.pin`), C1c (hash malformado)
- failure modes: C1, C1b, C1c, C6, C27
- idempotency and retry: C9 (retry durante bloqueio), C25 (reinstalar)
- authorization: C3, C5, C16 (guard novo em `/api/**`; nada existente cobria)
- concurrency and ordering: C12
- data lifecycle: C14, C15; limpeza de expiradas fora de escopo
- external-dependency failure: n/a - sem serviço externo; falha do H2 já é tratada pelo Spring, como hoje
- state transitions: C8, C9, C11
- observability: C13 (nunca loga PIN); log `WARN` por bloqueio com IP sem PIN, sem teste próprio

## Coverage

| Set (size) | Member -> proof | Unproven |
| --- | --- | --- |
| respostas de `POST /api/auth/pin` (4) | 204 C4 · 400 C7 · 401 C6 · 429 C8 | - |
| estados do limite (3) | Livre -> Contando C11 · Contando -> Bloqueado C8 · Bloqueado -> Livre C9 | - |
| regras do guard (2 lados) | fora de `/api/auth/**` bloqueado C3 · `/api/auth/**` liberado C4 | - |
| pontos de subida (2 montagens) | aplicação principal C1, C2b · montagem de teste `TestAuthConfiguration` C28 | - |
| telas do PIN (4 estados) | 401 que mostra a tela C17 · 204 que volta C18 · 401 mensagem C19 · 429 mensagem C20 | - |

- Claims naming a status code or response shape: C3, C4, C6, C7, C8, C16, C21 - each has a proof that crosses the MockMvc boundary (`AccessPinApiTest`, `H2ConsoleDisabledTest`)
- Claims about the Windows scripts are proven by reading the script text, not by running it: C24 to C27 - gap declared; see Verify
- Claims about the browser (C17 to C20) are proven by a script outside the repo, not by a committed test - gap declared

## Handoff

Single batch. Estimated reading: ~37 KB total (≈ 9k tokens), far under 150k. No handoff is needed; the build runs in one agent.

## Verificação

Feita na própria sessão de build, sem um Verifier de contexto separado (ver nota abaixo). Todas as provas foram executadas.

| Check | Resultado | Como |
| --- | --- | --- |
| C1 | PASS | `PinConfigurationTest#missingHashFailsStartupWithInstructions` |
| C1b | PASS | `java -jar` sem `FINANCE_PIN_HASH` saiu com código 1 e a mensagem `Defina FINANCE_PIN_HASH` |
| C1c | PASS | `PinConfigurationTest#hashWithoutBcryptPrefixFailsStartup` |
| C2 | PASS | `PinHashCliTest#printsBcryptHashAndReturnsZero`; comando real imprimiu `$2a$10$...` e saiu com 0 |
| C2b | PASS | porta 8080 fechada depois de `--gerar-hash-pin` |
| C3 | PASS | `AccessPinApiTest#apiWithoutSessionReturns401WithPinMessage`; `curl` em `/api/profiles` deu 401 `PIN necessário` |
| C4 | PASS | `AccessPinApiTest#correctPinReturns204AndSessionCookie`; `curl` mostrou `HttpOnly; SameSite=Strict; Max-Age=7776000`, sem `Secure` |
| C5 | PASS | mesmo teste; `curl` com o cookie em `/api/profiles` deu 200 |
| C6 | PASS | `AccessPinApiTest#wrongPinReturns401WithoutCookie` |
| C7 | PASS | `AccessPinApiTest#blankPinReturns400WithValidationShape` |
| C8 | PASS | `AccessPinApiTest#fifthWrongAttemptBlocksIpEvenWithCorrectPin` |
| C9 | PASS | `PinAttemptLimiterTest#attemptsWhileBlockedDoNotExtendBlock`, `#blockEndsFifteenMinutesAfterFifthWrong` |
| C10 | PASS | `AccessPinApiTest#blockIsPerIp` |
| C11 | PASS | `PinAttemptLimiterTest#correctPinResetsCount` |
| C12 | PASS | `PinAttemptLimiterTest#concurrentWrongAttemptsProduceAtMostFiveUnauthorized` (5 `WRONG`, 5 `BLOCKED`) |
| C13 | PASS | `PinLogTest#neverLogsPinOrHash` |
| C14 | PASS | `AccessSessionServiceTest#sessionSurvivesServiceRecreation`; e reinício real do `.jar` com o mesmo cookie deu 200 |
| C15 | PASS | `AccessSessionServiceTest#sessionExpiresAfterNinetyDays` |
| C16 | PASS | `AccessPinApiTest#tamperedCookieReturns401`; cookie falso no `curl` deu 401 |
| C17 | PASS | roteiro Playwright (fora do repo): tela com campo e botão Entrar |
| C18 | PASS | roteiro Playwright: PIN certo some a tela, dashboard aparece sem recarregar a página |
| C19 | PASS | roteiro Playwright: `PIN incorreto` abaixo do campo |
| C20 | PASS | roteiro Playwright: 429 mostra `Muitas tentativas. Tente novamente em 15 minutos` |
| C21 | PASS | `H2ConsoleDisabledTest#h2ConsoleIsNotServed`; `curl` deu 404 |
| C22 | PASS | `npm run build` + `mvn package`; `GET /` deu 200 `text/html`, o asset `/assets/*.js` deu 200 |
| C23 | PASS | com o `.jar` no ar, `/api/profiles` sem sessão deu 401 e `/` deu 200 |
| C24 | PASS (texto) | `WindowsScriptsTest#installerRegistersLogonTaskWithWorkingDirectory`. Script não executado: sem PowerShell aqui |
| C25 | PASS (texto) | `WindowsScriptsTest#installerIsIdempotent` |
| C26 | PASS (texto) | `WindowsScriptsTest#removerUnregistersTask` |
| C27 | PASS (texto) | `WindowsScriptsTest#installerFailsWithoutJdkBeforeRegistering` |
| C28 | PASS | `env -u FINANCE_PIN_HASH mvn -B test`: 64 testes, 0 falhas |
| C29 | PASS | `data/finance.mv.db` ausente antes e depois de `mvn test`; `TestDatabaseIsolationTest` passa |
| C30 | PASS | `grep` nos quatro termos em `docs/acesso-pela-casa.md` |

**Gaps declarados**

- C24 a C27 provam o texto do script, não a execução. Quem executa é o usuário no PC de casa. O `WindowsScriptsTest` não substitui rodar a tarefa agendada de verdade.
- C17 a C20 têm roteiro Playwright fora do repositório (`scratchpad`), não commitado. Reproduzi-lo exige Chromium e um servidor no ar.
- Não houve Verifier de contexto separado: a sessão de build fez a verificação. Se quiser a passada independente, peça e eu despacho um agente fresco sobre o intervalo de commits.

**Desvios registrados**

- Limite de tentativas em memória (Unresolved 14, default). Reiniciar o servidor zera o bloqueio.
- Sem limpeza de linhas expiradas em `sessao`.
- O nome do cookie `FINANCE_SESSION` e o `error` `Não autorizado` / `Muitas requisições` foram escolhidos como defaults (Unresolved 7).
- Em `ui`, os critérios da tela de PIN ficaram sem desenho vinculado (Unresolved 5).
