import { test } from 'node:test'
import assert from 'node:assert/strict'
import { blocksForDuration } from './agenda.ts'

test('blocksForDuration: 45 min ocupa 1 bloco', () => {
  assert.equal(blocksForDuration(45), 1)
})

test('blocksForDuration: limite exato de bloco não sobe', () => {
  assert.equal(blocksForDuration(60), 1)
  assert.equal(blocksForDuration(120), 2)
})

test('blocksForDuration: acima do limite arredonda para cima', () => {
  assert.equal(blocksForDuration(61), 2)
  assert.equal(blocksForDuration(150), 3)
})
