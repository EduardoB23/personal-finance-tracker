# Registro de Decisões

Decisões tomadas na sessão de alinhamento de 10/10/2026 (entrevista "grilling"). Cada decisão prevalece sobre trechos divergentes de outros documentos até que eles sejam atualizados.

## Objetivo e plataforma

| # | Decisão | Alternativa rejeitada |
|---|---|---|
| D1 | O projeto é **uso real pela família + estudo**; o uso real guia as prioridades. | Projeto apenas de estudo, sem compromisso de entrega |
| D2 | **Servidor único no PC de casa**, acessado pelos celulares via Wi‑Fi local. | App por dispositivo sincronizando por import/export (exigiria reescrever a arquitetura) |
| D3 | O Spring Boot **serve o build do React**: um único `.jar`, porta 8080. Em desenvolvimento, `vite dev` com proxy. | Dois processos em uso real |
| D4 | O servidor **sobe automaticamente com o Windows** (Agendador de Tarefas ou serviço). Hardware dedicado fica para o futuro. | Iniciar manualmente |
| D5 | Frontend **responsivo, pensado primeiro para celular**, com atalho na tela inicial. Sem PWA, porque HTTP na LAN não permite service worker. | PWA com HTTPS local e certificado próprio |

## Segurança

| # | Decisão | Alternativa rejeitada |
|---|---|---|
| D6 | **PIN familiar único** em tela própria, com cookie de sessão de 90 dias por aparelho. O PIN fica fora do código, em variável de ambiente e com hash. | HTTP Basic (pede de novo com frequência no celular); login individual |
| D7 | **Console do H2 desligado.** | Console exposto na rede local com usuário `sa` e senha vazia |
| D8 | Perfis continuam servindo só para identificar o responsável pelo lançamento; não existem contas de usuário. | Login por membro |

## Stack e código (divergências entre docs e código)

| # | Decisão | Alternativa rejeitada |
|---|---|---|
| D9 | **Java 21** (o `pom.xml` sai do 17). | Manter o 17 |
| D10 | IDs **`Long`** com auto-incremento. Com servidor único, não há mescla entre dispositivos que justifique UUID. | UUID |
| D11 | Pacote **`com.finance.tracker`**. | Renomear para `com.personalfinancetracker` |
| D12 | Schema versionado com **Flyway**, começando de um banco limpo (o banco atual só tem dados de teste). | `ddl-auto: update` + migração manual |
| D13 | Frontend: **Tailwind agora**; **Zustand** entra com o primeiro estado global ("quem sou eu"). | Instalar tudo de antemão; CSS puro |

## Domínio

| # | Decisão | Alternativa rejeitada |
|---|---|---|
| D14 | **`Category` é uma entidade** com tipo (Receita/Despesa) e lista padrão inicial. Transações e orçamentos referenciam a categoria por ID. | Texto livre; enum fixo |
| D15 | Categoria em uso é **arquivada**, nunca apagada; continua aparecendo no histórico. | Exclusão física |
| D16 | Orçamento é **por categoria e por mês, para a família toda** (sai o vínculo com perfil). | Orçamento por perfil |
| D17 | Compras no cartão contam **no mês da compra** no extrato e no orçamento (cada parcela conta no seu mês); o saldo da conta só muda quando a fatura é paga. | Contar pelo mês da fatura |
| D18 | **Pagamento de fatura** debita a conta escolhida, marca a fatura como paga e devolve o limite. Não conta como despesa, funciona como transferência. | Cartão apenas informativo |
| D19 | Lançamento errado se corrige com **exclusão com estorno** (saldo, fatura e limite). Excluir uma compra parcelada remove todas as parcelas. A edição completa fica para depois. | Edição completa já no MVP |
| D28 | **Contas e cartões são da família**, sem perfil dono. Qualquer perfil lança em qualquer conta ou cartão; o perfil só registra quem fez o lançamento. | Dono por conta (impede lançar no cartão do outro ou na conta conjunta) |
| D29 | **Cada ocorrência recorrente aplica seu efeito ao vencer** (saldo ou fatura) e fica ligada à regra. Excluir uma ocorrência apaga só aquele mês, que não é recriado; excluir a regra encerra as futuras e mantém as passadas. Cartão aceita recorrência. | Manter o comportamento atual, em que só a primeira ocorrência mexe no saldo |

## Experiência de uso

| # | Decisão |
|---|---|
| D20 | "Quem sou eu" é escolhido **uma vez por aparelho**, fica salvo localmente e pode ser trocado. |
| D21 | O lançamento rápido mostra **tipo (Despesa por padrão), valor, categoria e "pagar com"** (contas e cartões numa lista só), com a data de hoje. Em "mais opções" ficam descrição, data, parcelas (só para cartão), recorrência e transferência. |
| D22 | O extrato do mês mostra a **família toda com filtro por pessoa**, os totais de entradas, saídas e saldo, e navegação entre meses. |

## Dados e backup

| # | Decisão | Alternativa rejeitada |
|---|---|---|
| D23 | O **H2 é a fonte da verdade**. Planilha só como cópia de leitura (`.xlsx`, uma aba por entidade), numa entrega futura. | Planilha como armazenamento principal |
| D24 | **Backup automático diário em JSON** numa pasta configurável (padrão: OneDrive), guardando os últimos 30 arquivos. Import/export manual continua existindo. | Apenas export manual |
| D25 | Recorrência e backup também rodam **ao iniciar o servidor**, recuperando o que ficou pendente. Falha no backup gera aviso no app. | Somente no horário agendado |

## Processo

| # | Decisão | Alternativa rejeitada |
|---|---|---|
| D26 | Fluxo **tlc** (`tlc-discover` → `tlc-plan` → `tlc-implement`), com tasks em `.tasks/` e checks em `.checks/`. | Fluxo `.agent/` (spec/plan/tasks/checklist) + Linear |
| D27 | Trabalho em branches, com merge na `main` depois. Commits seguem Conventional Commits, em PT-BR. | Commits direto na `main` |
| D30 | **O fluxo começa no Linear** (time COD, projeto Personal Finance Tracker): toda entrega ou ajuste nasce como issue, a branch usa o nome sugerido pela issue (`gitBranchName`) e a issue é atualizada a cada marco (planejada, implementada e verificada, PR aberto). Revisa a D26, que tinha retirado o Linear. | Repositório como única fonte de acompanhamento (sem histórico fora do código) |

## Ordem das entregas

1. **Base**: Java 21, atualização de `docs/` e do `copilot-instructions.md`, criação do `CLAUDE.md`
2. **Fundação de dados**: Flyway com banco limpo, `Category`, orçamento da família, exclusão com estorno
3. **Acesso pela casa**: PIN, console do H2 desligado, frontend servido pelo `.jar`, inicialização com o Windows
4. **Lançamento rápido**: Tailwind, "quem sou eu" (Zustand), cadastro mínimo de perfis, contas e cartões, formulário rápido, extrato do mês
5. **Pagamento de fatura**
6. **Backup automático**
7. *Depois*: edição de lançamentos, exportação `.xlsx`, telas de orçamento e metas

## Em aberto (decidir no `tlc-plan` da entrega correspondente)

- Vai existir pagamento parcial de fatura? (entrega 5)
- Quantas tentativas erradas de PIN antes de bloquear? (entrega 3)
