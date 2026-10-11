// Regras de orçamento e meta na tela: quando o formulário pode ser enviado. Puro, para teste sem navegador.
import { parseAmount } from './quickEntry.ts'

export function canCreateBudget(categoryId: number | null, limit: string): boolean {
  const value = parseAmount(limit)
  return categoryId !== null && Number.isFinite(value) && value >= 0 && limit.trim() !== ''
}

export function canCreateGoal(title: string, target: string, profileId: number | null): boolean {
  const value = parseAmount(target)
  return title.trim() !== '' && profileId !== null && Number.isFinite(value) && value >= 0 && target.trim() !== ''
}

export function canContribute(amount: string): boolean {
  const value = parseAmount(amount)
  return Number.isFinite(value) && value > 0
}

export function progressPercent(current: number, target: number): number {
  if (target === 0) return 0
  return Math.min((current / target) * 100, 100)
}
