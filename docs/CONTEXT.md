# Contexto Técnico do Personal Finance Tracker

Este arquivo define os padrões técnicos, arquitetura e convenções adotadas pelo projeto. Ele deve ser consultado e atualizado sempre que uma decisão técnica importante for tomada.

> As decisões registradas em [`decisions.md`](decisions.md) prevalecem sobre este arquivo em caso de divergência.

## 1. Visão de Arquitetura

O projeto é um **monolito modular** com duas camadas principais:
- **Backend Java 21 + Spring Boot**: responsável por regras de negócio, persistência e APIs REST locais.
- **Frontend React + TypeScript**: responsável pela interface, visualização e interação do usuário.

A arquitetura foi escolhida para manter:
- desempenho local,
- alta privacidade,
- baixo custo,
- simplicidade de execução em ambiente doméstico.

## 2. Stack Tecnológica

### Backend
- **Linguagem:** Java 21
- **Framework:** Spring Boot (última versão estável compatível com Java 21)
- **Build:** Maven
- **Banco de Dados Local:** H2 Database em modo arquivo (`jdbc:h2:file:./data/finance`), schema versionado com Flyway (D12)
- **Objetivo:** persistência local e regras de negócio centralizadas

### Frontend
- **Framework:** React + TypeScript
- **Build Tool:** Vite
- **Estilização:** Tailwind CSS
- **Estado global:** Zustand, adotado a partir do primeiro estado global (D13)
- **Comunicação:** REST API local com o backend Spring Boot
- **Distribuição:** o build do React é servido pelo próprio `.jar` do Spring Boot na porta 8080 (D3)

## 3. Estrutura de Pastas Recomendada

### Backend
- `src/main/java/com/finance/tracker` (D11)
  - `account` : contas bancárias, cartões e faturas
  - `transaction` : lançamentos, transferências, recorrência e parcelamento
  - `budget` : limites mensais por categoria
  - `goal` : metas financeiras
  - `profile` : membros da família e responsáveis por transações
  - `backup` : exportação e importação de dados
  - `common` : tratamento de erros, conversores e utilitários comuns

### Frontend
- `src/`
  - `components` : componentes reutilizáveis e telas
  - `features` : módulos por domínio (accounts, transactions, budgets, goals)
  - `hooks` : hooks personalizados para lógica de UI
  - `store` : Zustand stores
  - `services` : chamadas à API REST
  - `types` : interfaces TypeScript
  - `utils` : formatação de moeda, datas e helpers

## 4. Regras de Domínio

- **ID:** toda entidade possui um identificador `Long` com auto-incremento (D10).
- **Responsável familiar:** cada transação deve indicar quem a realizou, mesmo sem login.
- **Transações:**
  - `id`
  - `amount`
  - `date`
  - `description`
  - `categoryId`
  - `accountId`
  - `type` (`INCOME`, `EXPENSE`, `TRANSFER`)
  - `responsibleId`
  - `status` (opcional para parcelas ou previsões)
- **Parcelamento:** deve ser modelado com uma estrutura que relacione parcelas a um grupo pai.
- **Recorrência:** deve ser persistida como regra de repetição mensal e não apenas como cópia manual.
- **Metas financeiras:** devem registrar valor inicial, objetivo e progresso acumulado.

## 5. Regras de Código

- **Código-fonte:** em inglês.
- **Comentários, documentação e mensagens de commit:** em português (PT-BR).
- **TypeScript:** sem `any`.
- **Java 21:** preferir `record`, `sealed`, `switch` pattern matching e streams quando apropriado.
- **Spring Boot:** separar módulos por domínio, evitar acoplamento excessivo.
- **Frontend:** componentes funcionais, reutilizáveis e com estado mínimo.

## 6. Regras de Negócio e Produto

- O sistema é **offline-first**: roda num servidor único no PC de casa, acessado pelos celulares via Wi‑Fi local (D2).
- Não há contas de usuário; o acesso é protegido por um PIN familiar único com sessão longa por aparelho (D6, D8).
- Exportação e importação manual de CSV/JSON continuam disponíveis, e o backup automático diário vai para uma pasta configurável (D24).
- Nenhum dado financeiro deve sair do ambiente local sem consentimento explícito do usuário.

## 7. Estado Atual do Projeto

- A arquitetura foi definida como **monolito modular**.
- O backend será responsável pela persistência dos dados e regras centrais.
- O frontend será responsável apenas pela interface e interação do usuário.
- O backend do MVP está implementado (perfis, contas, cartões e faturas, transações, recorrência, parcelamento, orçamentos, metas e backup).
- O frontend ainda é um dashboard somente leitura.
- Próximos passos: ver a ordem das entregas em [`decisions.md`](decisions.md).
