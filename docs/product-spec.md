# Especificação de Produto e Requisitos (PRD)

Este documento descreve o escopo funcional, regras de negócio e decisões de arquitetura do **Personal Finance Tracker**, acordadas durante a etapa de concepção inicial (conduzida via processo de entrevista de requisitos).

> Revisado em 10/10/2026: as decisões em [`decisions.md`](decisions.md) prevalecem em caso de divergência.

---

## 🎯 1. Visão Geral e Público-Alvo

*   **Objetivo:** Oferecer uma ferramenta prática, rápida e visual para acompanhamento de receitas, despesas, planejamento de viagens, compras e investimentos a médio prazo.
*   **Público-Alvo:** Familiar (uso focado no controle das contas do lar).
*   **Modelo de Negócio / Custo:** 100% gratuito e open-source, utilizando apenas infraestrutura e recursos de computação local ou camadas gratuitas de nuvem no futuro.
*   **Privacidade:** Abordagem *offline-first*. Nenhum dado financeiro sai do dispositivo do usuário sem o seu consentimento ativo (backup manual).

---

## 👥 2. Multi-Usuário (Abordagem Sem Login)

Para manter a simplicidade arquitetural e custo zero no MVP:
*   A aplicação não exigirá criação de conta. O acesso pela rede de casa é protegido por um **PIN familiar único** (D6).
*   O controle familiar será realizado por meio de **perfis individuais locais**.
*   Cada transação cadastrada conterá um campo identificador (ex: "Responsável") mapeando qual membro da família realizou aquela receita, despesa ou transferência.
*   Desta forma, os saldos podem ser consolidados de forma global ou filtrados por integrante na interface.
*   Cada aparelho escolhe uma vez "quem sou eu", e esse perfil vem preenchido nos lançamentos (D20).

---

## 🛠️ 3. Requisitos Funcionais (Escopo do MVP)

### 3.1. Lançamentos e Categorização
*   Cadastro de **Receitas** (Entradas) e **Despesas** (Saídas).
*   Categorias personalizáveis cadastradas como entidade, com tipo Receita ou Despesa e lista padrão inicial (ex: Alimentação, Moradia, Transporte, Lazer). Categorias em uso são arquivadas, nunca apagadas (D14, D15).
*   Atribuição de cada lançamento a um perfil familiar responsável.

### 3.2. Contas Bancárias e Cartões de Crédito
*   Cadastro de múltiplas Contas Bancárias (Corrente, Poupança, Carteira, Investimentos) com controle de saldos individuais.
*   Cadastro de Cartões de Crédito, com controle de limite total, limite disponível, data de vencimento e data de fechamento da fatura.
*   As compras feitas em cartão devem ser consolidadas na fatura do mês de vencimento correspondente.
*   No extrato e no orçamento, a compra conta no mês em que foi feita; o saldo da conta só muda quando a fatura é paga (D17).
*   O pagamento de fatura debita a conta escolhida, devolve o limite e não conta como despesa (D18).

### 3.3. Transferências
*   Registro de transações de transferência entre contas (ex: "Retirada de R$ 500,00 da Conta Corrente para aplicação na Conta Investimentos"), alterando o saldo das duas contas simultaneamente sem afetar o resultado do fluxo de despesa/receita global.

### 3.4. Recorrência, Parcelamento e Fluxos Futuros
*   **Lançamentos Recorrentes:** Despesas ou receitas fixas mensais (aluguel, salário, assinaturas de streaming) que se repetem automaticamente a cada mês.
*   **Parcelamento:** Compras parceladas (ex: 12x de R$ 100,00) que provisionam despesas nos meses futuros de forma automática.
*   **Previsões Futuras (Projeções):** Capacidade de lançar receitas/despesas futuras planejadas para simular o saldo estimado dos próximos meses, auxiliando no planejamento de viagens ou investimentos.

### 3.5. Orçamento Mensal (Budgeting)
*   Definição de limites de gastos mensais por categoria para a família toda (ex: limite de R$ 800,00 para Alimentação) (D16).
*   Painel visual mostrando a porcentagem consumida do orçamento planejado em tempo real.

### 3.6. Metas Financeiras (Goals)
*   Criação de objetivos de poupança (ex: "Viagem de Fim de Ano - R$ 5.000,00").
*   Acompanhamento visual do progresso de acumulação de valores direcionados para essa meta.

---

## 💾 4. Arquitetura de Dados e Sincronização

*   **Banco de Dados Local (Monolito Modular):** Persistência local em arquivo utilizando **H2 Database** gerenciado pelo backend em **Java 21 / Spring Boot** (`jdbc:h2:file:./data/finance`), garantindo total privacidade e execução offline.
*   **Acesso da Família:** o servidor roda no PC de casa e sobe com o Windows; os celulares acessam o app pelo navegador via Wi‑Fi local, sem necessidade de sincronizar dados entre aparelhos (D2, D4).
*   **Backup:**
    *   Backup automático diário em JSON numa pasta configurável (padrão: OneDrive), guardando os últimos 30 arquivos (D24).
    *   A área de configurações mantém **"Exportar Dados"** e **"Importar Dados"** (CSV ou JSON) para restauração manual. A importação substitui todo o estado local.

---

## 🚀 5. Evolução Futura (Roadmap)

*   **Fase 2 (Sincronização Automatizada sem Custo):** Integração com a API do Google Drive do próprio usuário. O app salvará e lerá os dados de um arquivo oculto no Drive pessoal da família, garantindo sincronização automática sem que o desenvolvedor precise pagar por servidores de banco de dados.
*   **Fase 3 (Análises Avançadas):** Dashboards de projeção anual detalhados e alertas inteligentes de desvios padrão em gastos.
