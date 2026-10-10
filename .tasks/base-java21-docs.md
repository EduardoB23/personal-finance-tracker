# Entrega 1 - Base: Java 21 e instruções de agentes alinhadas

> Build this with **tlc-implement**.
> Every criterion below becomes a check with a proof, referenced by its number. Nothing under
> `Unresolved` gets settled while building.

## Intent

Os documentos que orientam o projeto mandam usar Java 21, mas o `pom.xml` compila em Java 17 e o `JAVA_HOME` da máquina aponta para o JDK 17. Além disso, o `.github/copilot-instructions.md` obriga o fluxo `.agent/` com spec/plan/tasks/checklist e o uso do Linear, enquanto o trabalho real segue o fluxo tlc (`.tasks/` + `.checks/`). Também não existe `CLAUDE.md`. Quem paga é qualquer agente ou pessoa que abre o repositório: ou segue instruções que contradizem o processo decidido (D26), ou escreve código Java 17 num projeto que promete recursos de Java 21 (D9).

Com esta entrega, o backend compila e passa nos testes em Java 21, e qualquer agente (Copilot ou Claude) que abrir o repositório encontra uma única instrução de processo coerente, apontando para `docs/decisions.md` como fonte das decisões.

6 critérios em 3 fatias · 1 porta sem retorno · 0 abertos, dos quais 0 bloqueiam

## Criteria

### Backend em Java 21

1. Quando `mvn test` roda com um JDK 21, então os 15 testes existentes passam (0 falhas, 0 erros).
2. Quando o backend é compilado, então as classes em `target/classes` têm versão de bytecode 65 (Java 21), conforme `javap -v`.

### Instruções de agentes coerentes

3. Quando alguém lê `.github/copilot-instructions.md`, então o arquivo não contém nenhuma ocorrência de `Linear`, `mcp_linear` ou `.agent/` e descreve o fluxo `tlc-discover` → `tlc-plan` → `tlc-implement` com tasks em `.tasks/` e checks em `.checks/`.
4. Quando alguém lê `.github/copilot-instructions.md`, então a seção de stack diz Java 21, `com.finance.tracker`, IDs `Long` e Flyway como forma de evoluir o schema, sem contradizer `docs/decisions.md`, e o arquivo aponta `docs/decisions.md` como prevalecente em caso de divergência.
5. Quando um agente abre o repositório, então existe um `CLAUDE.md` na raiz que aponta para `docs/decisions.md`, `docs/CONTEXT.md` e `docs/product-spec.md`, traz os comandos `mvn test`, `mvn spring-boot:run` e `npm run dev` (em `frontend/`), repete as regras de idioma (código em inglês; docs e commits em PT-BR, Conventional Commits) e manda nunca versionar `data/`.

### Testes isolados do banco real

6. Quando `mvn test` roda, então o arquivo `data/finance.mv.db` não é criado nem modificado (mesmo hash e data de modificação antes e depois), e os testes usam um H2 em memória.

## Out of scope

- Correção de encoding no `App.tsx` - verificado: o arquivo é UTF-8 válido, e o texto quebrado era só a leitura do PowerShell com a codificação errada. `docs/decisions.md` foi corrigido.
- Flyway, `Category`, orçamento da família e exclusão com estorno - entrega 2.
- Alterar o `JAVA_HOME` da máquina - o próprio usuário troca para `C:\Program Files\Java\jdk-21.0.11` (decidido: opção a, sem Maven Toolchains).

## Observable

| Surface | Decision | Landing |
| --- | --- | --- |
| documento `.github/copilot-instructions.md` | estrutura | 3, 4 |
| documento `.github/copilot-instructions.md` | tom e profundidade | existing - tom imperativo e seções numeradas já em uso |
| documento `.github/copilot-instructions.md` | o que o leitor faz em seguida | 3 |
| documento `CLAUDE.md` | estrutura | 5 |
| documento `CLAUDE.md` | tom e profundidade | 5 |
| documento `CLAUDE.md` | o que o leitor faz em seguida | 5 |

## Swept

- validation: n/a - nenhuma entrada de usuário muda nesta entrega
- failure modes: n/a - uma build com o JDK errado já falha por conta própria no Maven
- idempotency and retry: n/a - sem operações repetíveis novas
- authorization: n/a - sem superfície de acesso nova
- concurrency and ordering: n/a - sem fluxos concorrentes novos
- data lifecycle: 6
- external-dependency failure: n/a - nenhuma dependência nova; o Lombok gerenciado pelo Spring Boot 3.2.5 já suporta Java 21
- state transitions: n/a - nenhum ciclo de vida muda
- observability: n/a - logs padrão do Spring Boot, sem mudança

## Impact

| Front | What changes |
|---|---|
| domain | nothing |
| stored data | nothing to migrate |
| build | o requisito mínimo passa a ser JDK 21; quem compila com o JAVA_HOME atual (JDK 17) recebe erro de versão de release |
| process | o fluxo `.agent/` + Linear deixa de existir; nenhum arquivo `.agent/` existe hoje no repositório |

## Decided

| Decision | Shape | Alternative rejected |
|---|---|---|
| Java 21 como versão mínima de build | `<java.version>21</java.version>` no `pom.xml` | Ficar no 17 - não permite os recursos de Java 21 prometidos nas regras de código (D9) |

## Sources

- `docs/decisions.md` - D9 (Java 21), D10 e D11 (IDs `Long`, pacote), D12 (Flyway), D26 (fluxo tlc), D27 (branches e Conventional Commits) e a ordem das entregas
- `docs/CONTEXT.md` - regras de código e idioma
- Conversa de 10/10/2026 (sem endereço): "Q5 a" - fluxo tlc; "Q10 a" - subir para o 21; "1 - sim, incluir" - isolar o banco dos testes; "2 - a" - usuário troca o JAVA_HOME

This task is the record of decision. If a linked document diverges, ask before building.

## Unresolved

| # | Kind | Question | Until answered |
|---|---|---|---|
| None | | | |
