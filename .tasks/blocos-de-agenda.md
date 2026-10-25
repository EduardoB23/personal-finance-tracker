# Calcular blocos de agenda por duração

> Build this with **tlc-implement** (`.claude/skills/tlc-implement/SKILL.md`).
> Every criterion below becomes a check with a proof, referenced by its number. Nothing under
> `Unresolved` gets settled while building.

## Intent

A agenda trabalha com blocos de 1 hora cheia, mas hoje não existe nenhuma função que converta uma duração em minutos na quantidade de blocos ocupados. Quem for montar a agenda teria de repetir a conta, e a regra de arredondamento poderia divergir. A issue COD-44 é também o teste de ponta a ponta do pipeline da fábrica; a evidência de urgência não foi dada.

A mudança: existe `blocksForDuration(minutes: number): number` em `frontend/src/lib/agenda.ts`, que devolve quantos blocos de 60 minutos a duração ocupa, sempre arredondando para cima, com teste ao lado.

3 criteria in 1 slice · 0 one-way doors · 2 open, of which 0 block

## Criteria

1. Dado 45 minutos, quando `blocksForDuration(45)` é chamada, então devolve `1`.
2. Dado um limite exato de bloco, quando `blocksForDuration(60)` é chamada, então devolve `1`; e `blocksForDuration(120)` devolve `2`.
3. Dado um valor logo acima do limite, quando `blocksForDuration(61)` e `blocksForDuration(150)` são chamadas, então devolvem `2` e `3`.

## Out of scope

- Durações por categoria de serviço - valores ainda provisórios, ficam para outra issue.
- Persistência, rotas, banco de dados e lint - fora do escopo da issue.
- Entradas inválidas (zero, negativos, fracionários, `NaN`) - só durações inteiras positivas estão no escopo; o comportamento nelas fica indefinido.
- Uso da função em alguma tela - a issue só pede a função e o teste.

## Observable

None - no user-facing surface

## Swept

- validation: n/a - entradas inválidas estão fora do escopo da issue
- failure modes: n/a - função pura, sem I/O
- idempotency and retry: n/a - função pura
- authorization: n/a - sem acesso a dados
- concurrency and ordering: n/a - função pura, sem estado
- data lifecycle: n/a - nada é persistido
- external-dependency failure: n/a - sem dependências
- state transitions: n/a - sem estado
- observability: n/a - sem log pedido

## Impact

| Front | What changes |
|---|---|
| domain | new term: `blocksForDuration` - blocos de 60 min ocupados por uma duração, vive em `frontend/src/lib/agenda.ts` |
| stored data | nothing to migrate |

## Decided

None - função pura exportada em módulo novo, reversível por refatoração; sem dependência nova nem contrato externo.

## Sources

- Issue COD-44 (`/tmp/ai-ready/issue.md`) - regra de negócio, escopo e exemplos (45→1, 60→1, 61→2, 150→3).
- `frontend/package.json` - `npm test` roda `node --test 'src/**/*.test.ts'`, o padrão de `frontend/src/lib/*.test.ts`.

This task is the record of decision. If a linked document diverges, ask before building.

## Unresolved

| # | Kind | Question | Until answered |
|---|---|---|---|
| 1 | open | A issue diz `src/` e `npm test` na raiz, mas o `src/` da raiz é o backend Java e não há `package.json` na raiz. Onde fica a função? | Assumido `frontend/src/lib/agenda.ts` e `npm test` em `frontend/`, onde já vivem os módulos TS com `*.test.ts` ao lado. Não se cria `package.json` na raiz. |
| 2 | open | A issue cita `operacao/shared/decisoes.md` (2026-10-20), que não existe neste repositório. | Usada a regra e os exemplos copiados na própria issue; `docs/decisions.md` não é alterado. |
