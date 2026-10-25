# Calcular blocos de agenda por duração

Profile: light · handoff: off (tarefa pequena, um único agente de construção)

Sources:

- Issue COD-44 (`/tmp/ai-ready/issue.md`) - regra, escopo e exemplos.
- `.tasks/blocos-de-agenda.md` - critérios 1 a 3 e pontos `open`.

## Out of scope

- Durações por categoria de serviço - valores provisórios.
- Persistência, rotas, banco e lint.
- Entradas inválidas - só inteiros positivos.

## Landing

Novo módulo `frontend/src/lib/agenda.ts` com teste `agenda.test.ts` ao lado, no padrão dos demais módulos de `lib/`. Sem dependência nova.

None - função pura exportada, reversível por refatoração; nada aqui é difícil de reverter.

## Checks

### S1 - Blocos por duração · 2 files · ~1k

**C1** - 45 minutos ocupam 1 bloco (task 1)
Proof: `cd frontend && npm test` — teste `blocksForDuration: 45 min ocupa 1 bloco` em `src/lib/agenda.test.ts`

**C2** - 60 minutos ocupam 1 bloco e 120 ocupam 2 (task 2)
Proof: teste `blocksForDuration: limite exato de bloco não sobe` em `src/lib/agenda.test.ts`

**C3** - 61 minutos ocupam 2 blocos e 150 ocupam 3 (task 3)
Proof: teste `blocksForDuration: acima do limite arredonda para cima` em `src/lib/agenda.test.ts`

## Swept

- validation: not in scope - entradas inválidas fora da issue
- failure modes: n/a - função pura
- idempotency: n/a - função pura
- authorization: n/a - sem dados
- concurrency: n/a - sem estado
- data lifecycle: n/a - nada persistido
- dependency failure: n/a - sem dependências
- state transitions: n/a - sem estado
- observability: not in scope - sem log pedido

## Coverage

No set rows - não há conjunto enumerado além dos exemplos da issue (45, 60, 61, 150, mais 120), todos em C1-C3.
