# Contexto Técnico do Personal Finance Tracker

Este arquivo define os padrões técnicos, arquitetura e convenções adotadas pelo projeto. Ele deve ser consultado e atualizado sempre que uma decisão técnica importante for tomada.

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
- **Banco de Dados Local:** H2 Database em modo arquivo (`jdbc:h2:file:./data/finance`)
- **Objetivo:** persistência local e regras de negócio centralizadas

### Frontend
- **Framework:** React + TypeScript
- **Build Tool:** Vite
- **Estilização:** Tailwind CSS
- **Estado global:** Zustand
- **Comunicação:** REST API local com o backend Spring Boot

## 3. Estrutura de Pastas Recomendada

### Backend
- `src/main/java/com/personalfinancetracker` 
  - `accounts` : contas bancárias e cartões
  - `transactions` : lançamentos, transferências, recorrência e parcelamento
  - `budgets` : limites mensais por categoria
  - `goals` : metas financeiras
  - `reports` : relatórios e projeções
  - `family` : membros da família e responsáveis por transações
  - `config` : segurança, banco, CORS e configurações gerais
  - `shared` : utilitários, exceptions e validações comuns

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

- **ID:** toda entidade deve possuir um identificador único (`id`) no padrão UUID.
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

- O sistema é **offline-first** e não exige autenticação no MVP.
- O uso é local e familiar, com dados armazenados no próprio dispositivo.
- Exportação e importação manual de arquivos CSV/JSON são os mecanismos de sincronização do MVP.
- Nenhum dado financeiro deve sair do ambiente local sem consentimento explícito do usuário.

## 7. Estado Atual do Projeto

- A arquitetura foi definida como **monolito modular**.
- O backend será responsável pela persistência dos dados e regras centrais.
- O frontend será responsável apenas pela interface e interação do usuário.
- O projeto está em fase inicial de estruturação.
