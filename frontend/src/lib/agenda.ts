// A agenda trabalha com blocos de 1 hora cheia.
export const BLOCK_MINUTES = 60

// Quantos blocos de 60 minutos a duração ocupa, sempre arredondando para cima.
// Só durações inteiras positivas estão no escopo.
export function blocksForDuration(minutes: number): number {
  return Math.ceil(minutes / BLOCK_MINUTES)
}
