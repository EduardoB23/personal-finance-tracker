# Instruções Globais do Projeto - Personal Finance Tracker

Este documento estabelece as diretrizes obrigatórias de arquitetura, padrões de código, uso de ferramentas e processo de trabalho para o **Personal Finance Tracker**. Todos os agentes (Copilot, Claude, subagentes) devem seguir estritamente estas regras.

> Em caso de divergência entre este arquivo e [`docs/decisions.md`](../docs/decisions.md), as decisões registradas em `docs/decisions.md` prevalecem.

---

## 1. Visão Geral da Arquitetura & Stack Tecnológica

O projeto é um **Monolito Modular** *offline-first* para uso familiar: um servidor único roda no PC de casa e os celulares acessam pelo navegador via Wi‑Fi local. Privacidade total e custo zero de infraestrutura.

### Backend
- **Linguagem:** Java 21 (recursos modernos: *Records*, *Pattern Matching*, *Sealed Classes* e API de *Streams*).
- **Framework:** Spring Boot, com Maven como build.
- **Pacote base:** `com.finance.tracker`, com um subpacote por domínio.
- **Identificadores:** `Long` com auto-incremento em todas as entidades.
- **Persistência:** H2 em modo arquivo (`jdbc:h2:file:./data/finance`). O schema evolui com migrações Flyway, nunca com `ddl-auto: update`.
- **Testes:** usam um H2 em memória (`src/test/resources/application.properties`) e nunca tocam o banco real em `data/`.

### Frontend
- **Framework/Build:** React (via Vite) com TypeScript estrito (proibido o uso de `any`).
- **Estilização:** Tailwind CSS, com layout pensado primeiro para celular.
- **Gerenciamento de Estado:** Zustand, adotado só quando houver estado global de verdade (ex.: "quem sou eu" no aparelho).
- **Comunicação:** API REST servida pelo Spring Boot. Em uso real, o build do React é servido pelo próprio `.jar` na porta 8080.

---

## 2. Idioma e Convenções de Código

- **Código-Fonte (Nomes de Variáveis, Classes, Métodos e Tabelas):** Estritamente em **Inglês**.
- **Comentários, Documentação e Mensagens de Commit:** Em **Português (PT-BR)**.
- **Padrão de Commits (Conventional Commits):**
  - `feat: adiciona cálculo de faturas de cartão de crédito`
  - `fix: corrige arredondamento no parcelamento de despesas`
  - `docs: atualiza modelo de dados no diretório docs/`
- **Branches:** todo trabalho acontece numa branch própria e entra na `main` por merge.
- **Nunca versionar** `data/` (banco com dados financeiros reais), `target/` ou `frontend/dist/`.

---

## 3. Diretrizes de Documentação (Living Documentation)

- **`docs/decisions.md`:** registro das decisões de produto e arquitetura. Toda decisão nova ou revista entra ali, com a alternativa rejeitada.
- **`docs/CONTEXT.md`:** padrões técnicos e convenções. **`docs/product-spec.md`:** escopo funcional.
- Qualquer mudança significativa de arquitetura, regra de negócio ou modelo de dados deve ser refletida nesses arquivos no mesmo trabalho que a introduz.

---

## 4. Processo de Trabalho (Linear + fluxo tlc)

Todo trabalho começa numa issue do Linear (time `COD`, projeto `Personal Finance Tracker`). Para uma funcionalidade nova, refatoração ou correção complexa, não gere código direto. Siga o fluxo:

1. **Issue** - leia a issue pedindo só os campos necessários (`title`, `description`, `status`, `gitBranchName`). Sem issue, crie uma antes de começar.
2. **Branch** - crie a branch com o `gitBranchName` da issue, a partir da `main` (ou da branch da entrega anterior, se ainda não houve merge).
3. **`tlc-discover`** - quando a ideia ainda não tem forma: entrevista até chegar a um veredito e decisões literais.
4. **`tlc-plan`** - transforma o trabalho decidido numa task em `.tasks/<nome>.md`, com critérios observáveis e valores concretos.
5. **`tlc-implement`** - extrai o checklist em `.checks/<nome>.md`, com uma prova (teste ou comando) por check, implementa e passa por um verificador independente.
6. **PR** - abra o PR com o identificador da issue (ex.: `COD-11`) no título, para o Linear vincular branch e PR à issue.

**Atualização da issue:** um comentário curto por marco, nunca a cada commit. Os marcos são: task planejada (link do `.tasks/`, número de critérios, pontos abertos), implementação verificada (veredito e checks provados) e PR aberto. Mova o status junto: `In Progress` ao planejar, `In Review` com o PR aberto, `Done` após o merge. Não copie o conteúdo da task para a issue; aponte para o arquivo.

O que estiver em `Unresolved` numa task não é decidido durante a implementação: pergunte ao usuário.

---

## 5. Boas Práticas e Qualidade de Código

1. **Modularidade Rigorosa:** no backend, mantenha os módulos separados por domínio (`account`, `transaction`, `budget`, `goal`, `profile`, `backup`, `common`). Evite acoplamento cíclico.
2. **Dinheiro:** valores monetários sempre em `BigDecimal`, nunca `double`.
3. **Performance:**
   - No backend, utilize paginação e consultas otimizadas no H2.
   - No frontend, evite re-renderizações desnecessárias usando seletores granulares no Zustand e componentes memoizados quando aplicável.
4. **Tratamento de Erros:** exceções de negócio são tratadas de forma centralizada no Spring Boot (`@ControllerAdvice`), retornando mensagens claras em português para a interface.
