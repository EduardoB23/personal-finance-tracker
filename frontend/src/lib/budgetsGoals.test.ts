import { test } from 'node:test'
import assert from 'node:assert/strict'
import { canContribute, canCreateBudget, canCreateGoal, progressPercent } from './budgetsGoals.ts'

test('budgetNeedsCategoryAndLimit', () => {
  assert.equal(canCreateBudget(3, '500'), true)
  assert.equal(canCreateBudget(3, '0'), true)
  assert.equal(canCreateBudget(null, '500'), false)
  assert.equal(canCreateBudget(3, ''), false)
  assert.equal(canCreateBudget(3, '-1'), false)
})

test('goalNeedsTitleTargetAndProfile', () => {
  assert.equal(canCreateGoal('Viagem', '5000'), true)
  assert.equal(canCreateGoal('  ', '5000'), false)
  assert.equal(canCreateGoal('Viagem', ''), false)
  assert.equal(canCreateGoal('Viagem', '-1'), false)
})

test('contributionMustBePositive', () => {
  assert.equal(canContribute('10,50'), true)
  assert.equal(canContribute('0'), false)
  assert.equal(canContribute(''), false)
})

test('progressIsCappedAndSafeForZeroTarget', () => {
  assert.equal(progressPercent(250, 1000), 25)
  assert.equal(progressPercent(1200, 1000), 100)
  assert.equal(progressPercent(10, 0), 0)
})
