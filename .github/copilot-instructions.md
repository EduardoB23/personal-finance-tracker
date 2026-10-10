# Instruções Globais do Projeto - Personal Finance Tracker

Este documento estabelece as diretrizes obrigatórias de arquitetura, padrões de código, uso de ferramentas e integração para o **Personal Finance Tracker**. Todos os agentes (Copilot, subagentes) devem seguir estritamente estas regras.

---

## 1. Visão Geral da Arquitetura & Stack Tecnológica

O projeto é um **Monolito Modular** com arquitetura *offline-first* para uso local/familiar, garantindo privacidade total e custo zero de infraestrutura.

### Backend
- **Linguagem:** Java 21 (utilizando recursos modernos: *Records*, *Pattern Matching*, *Sealed Classes* e API de *Streams*).
- **Framework:** Spring Boot (versão estável mais recente compatível com Java 21).
- **Gerenciador de Dependências & Build:** Maven.
- **Persistência de Dados:** H2 Database configurado em modo arquivo (`jdbc:h2:file:./data/finance`), garantindo persistência local sem dependência de serviços externos.

### Frontend
- **Framework/Build:** React (via Vite) com TypeScript estrito (proibido o uso de `any`).
- **Estilização:** Tailwind CSS para uma interface moderna, limpa e responsiva.
- **Gerenciamento de Estado:** Zustand (escolhido por sua extrema performance, leveza e simplicidade em comparação a Redux ou Context API pesadas).
- **Comunicação:** API REST local servida pelo Spring Boot.

---

## 2. Idioma e Convenções de Código

- **Código-Fonte (Nomes de Variáveis, Classes, Métodos e Tabelas):** Estritamente em **Inglês** (padrão de mercado para legibilidade de código).
- **Comentários, Documentação e Mensagens de Commit:** Em **Português (PT-BR)**.
- **Padrão de Commits (Conventional Commits):**
  - `feat: adiciona cálculo de faturas de cartão de crédito`
  - `fix: corrige arredondamento no parcelamento de despesas`
  - `docs: atualiza modelo de dados no diretório docs/`

---

## 3. Diretrizes de Documentação (Living Documentation & DDD)

O projeto segue o princípio de **Documentação Viva**:
- **Manutenção do Diretório `docs/`:** Qualquer alteração significativa de arquitetura, novas regras de negócio ou expansão do modelo de dados deve ser refletida em arquivos Markdown dentro de `docs/`.
- **Decision Records (ADRs):** Decisões técnicas importantes devem ser documentadas ou atualizadas para evitar perda de contexto histórico.

---

## 4. Integração com o Linear (MCP)

O projeto utiliza a plataforma Linear para rastreamento de tarefas, issues e planejamento.
- **Uso Obrigatório das Ferramentas Linear (`mcp_linear_*`):**
  - **Consulta:** Sempre que for iniciar uma tarefa ou entender um requisito pendente, consulte as issues atribuídas ou o projeto no Linear.
  - **Rastreabilidade:** Ao concluir uma entrega ou parte importante de uma funcionalidade, utilize as ferramentas do Linear para atualizar o status da issue correspondente (ex: de *In Progress* para *Done*).
  - **Criação de Tarefas:** Se identificar um débito técnico, bug ou subtarefa durante o desenvolvimento, crie uma issue no Linear utilizando a ferramenta apropriada.

---

## 5. Boas Práticas e Qualidade de Código

1. **Modularidade Rigorosa:** No backend Spring Boot, mantenha os módulos bem separados por domínio (ex: `accounts`, `transactions`, `budgets`, `goals`, `reports`). Evite acoplamento cíclico.
2. **Performance em Primeiro Lugar:**
   - No backend, utilize paginação e consultas otimizadas no H2.
   - No frontend, evite re-renderizações desnecessárias usando seletores granulares no Zustand e componentes memoizados quando aplicável.
3. **Tratamento de Erros:** Exceções de negócio devem ser tratadas de forma centralizada no Spring Boot (`@ControllerAdvice`) retornando mensagens claras em português para a interface.


## 6. Diretrizes de Desenvolvimento Agêntico (SDD / ADD)

Sempre que o usuário solicitar uma nova funcionalidade, refatoração ou correção complexa, você NÃO deve gerar código diretamente de forma abrupta. Siga rigorosamente o fluxo em etapas abaixo, criando e atualizando os arquivos correspondentes na pasta `.agent/`:

1. **Especificação (Spec):** Crie um arquivo `spec.md` detalhando os requisitos funcionais, escopo e restrições técnicas.
2. **Plano de Arquitetura (Plan):** Crie um arquivo `plan.md` definindo a abordagem técnica, os componentes afetados e os padrões de design (ex: Java Spring Boot / Go Gin).
3. **Tarefas Atômicas (Tasks):** Crie um arquivo `tasks.md` dividindo o plano em passos sequenciais e independentes.
4. **Checklist de Aceitação (Checklist):** Crie um arquivo `checklist.md` com os critérios de validação e testes unitários/integrados necessários.

Só inicie a geração de código após o usuário validar e aprovar o plano e as tarefas descritas.
