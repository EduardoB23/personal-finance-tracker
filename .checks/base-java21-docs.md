# Entrega 1 - Base: Java 21 e instruções de agentes alinhadas

Sources:

- `.tasks/base-java21-docs.md` - critérios 1 a 6, escopo e decisões
- `docs/decisions.md` - D9 (Java 21), D10 a D12 (IDs `Long`, pacote, Flyway), D26 (fluxo tlc), D27 (branches, Conventional Commits)
- conversa de 10/10/2026 - "1 - sim, incluir" (isolar o banco dos testes); "2 - a" (o usuário troca o `JAVA_HOME`)

## Out of scope

- Encoding do `App.tsx` - o arquivo é UTF-8 válido
- Alterar o `JAVA_HOME` da máquina - quem faz é o usuário; as provas definem `JAVA_HOME` só no processo
- Flyway, `Category`, orçamento da família - entrega 2

## Landing

Muda o `pom.xml`, `.github/copilot-instructions.md`, cria o `CLAUDE.md` e uma configuração de teste em `src/test/resources`. Os testes `@SpringBootTest` existentes continuam como estão, sem anotação nova: a configuração de teste vale para todos por estar no classpath de teste.

| One-way door | Literal shape | Alternative rejected |
| --- | --- | --- |
| Java 21 como versão mínima de build | `<java.version>21</java.version>` | Java 17 - não permite os recursos de Java 21 das regras de código (D9) |
| Banco dos testes definido por arquivo no classpath de teste | `src/test/resources/application.properties` com `spring.datasource.url=jdbc:h2:mem:...`. Na mesma pasta, o Spring Boot dá precedência ao `.properties` sobre o `.yml`, então só a URL é sobrescrita e o resto do `application.yml` principal continua valendo | `src/test/resources/application.yml` - esconderia o `application.yml` principal inteiro (mesmo nome no classpath), e a configuração de JPA precisaria ser duplicada; `@ActiveProfiles("test")` em cada teste - um teste novo sem a anotação voltaria a usar o banco real |

- Nada mais nesta mudança é difícil de reverter

## Checks

### S1 - Backend em Java 21 · 1 arquivo · ~2 KB · ~1k

**C1** - Com `JAVA_HOME` no JDK 21, `mvn test` termina com 15 testes rodados, 0 falhas e 0 erros
Proof: `JAVA_HOME="/c/Program Files/Java/jdk-21.0.11" mvn -B test` (exit 0, linha `Tests run: 15, Failures: 0, Errors: 0`)

**C2** - As classes compiladas têm `major version: 65`
Proof: `"/c/Program Files/Java/jdk-21.0.11/bin/javap" -v target/classes/com/finance/tracker/FinanceApplication.class | grep "major version: 65"`

### S2 - Instruções de agentes coerentes · 2 arquivos · ~8 KB · ~2k

**C3** - `.github/copilot-instructions.md` não contém `Linear`, `mcp_linear` nem `.agent/`
Proof: `! grep -nE "Linear|mcp_linear|\.agent/" .github/copilot-instructions.md`

**C4** - `.github/copilot-instructions.md` descreve o fluxo tlc com `.tasks/` e `.checks/`
Proof: `grep -q "tlc-discover" .github/copilot-instructions.md && grep -q "tlc-plan" .github/copilot-instructions.md && grep -q "tlc-implement" .github/copilot-instructions.md && grep -q "\.tasks/" .github/copilot-instructions.md && grep -q "\.checks/" .github/copilot-instructions.md`

**C5** - A stack em `.github/copilot-instructions.md` diz Java 21, `com.finance.tracker`, IDs `Long` e Flyway, e aponta `docs/decisions.md` como prevalecente
Proof: `for p in "Java 21" "com.finance.tracker" "Long" "Flyway" "docs/decisions.md"; do grep -q "$p" .github/copilot-instructions.md || exit 1; done`

**C6** - `CLAUDE.md` existe na raiz e cita `docs/decisions.md`, `docs/CONTEXT.md`, `docs/product-spec.md`, `mvn test`, `mvn spring-boot:run`, `npm run dev`, as regras de idioma, Conventional Commits e a proibição de versionar `data/`
Proof: `for p in "docs/decisions.md" "docs/CONTEXT.md" "docs/product-spec.md" "mvn test" "mvn spring-boot:run" "npm run dev" "inglês" "PT-BR" "Conventional Commits" "data/"; do grep -q "$p" CLAUDE.md || exit 1; done`

### S3 - Testes isolados do banco real · 2 arquivos · ~3 KB · ~1k

**C7** - No contexto de teste, o `DataSource` usa uma URL `jdbc:h2:mem:`
Proof: `mvn -B test -Dtest=TestDatabaseIsolationTest` → `TestDatabaseIsolationTest.usesInMemoryDatabase`

**C8** - `mvn test` não modifica `data/finance.mv.db` (hash SHA-256 e data de modificação iguais antes e depois)
Proof: `before=$(sha256sum data/finance.mv.db; stat -c %Y data/finance.mv.db); JAVA_HOME="/c/Program Files/Java/jdk-21.0.11" mvn -B -q test; after=$(sha256sum data/finance.mv.db; stat -c %Y data/finance.mv.db); [ "$before" = "$after" ]`

## Swept

- validation: n/a - nenhuma entrada de usuário muda
- failure modes: n/a - build com o JDK errado já falha no próprio Maven
- idempotency and retry: n/a - nada repetível foi adicionado
- authorization: n/a - sem superfície de acesso nova
- concurrency and ordering: n/a - sem fluxo concorrente novo
- data lifecycle: C7, C8
- external-dependency failure: n/a - nenhuma dependência nova
- state transitions: n/a - nenhum ciclo de vida muda
- observability: n/a - sem mudança de logs

## Handoff

Não há handoff: S1 a S3 somam aproximadamente 4k tokens de leitura, bem abaixo do orçamento de 150k.
