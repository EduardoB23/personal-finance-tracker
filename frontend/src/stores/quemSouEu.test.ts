import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createQuemSouEuStore, QUEM_SOU_EU_KEY, resolveSelection } from './quemSouEu.ts'

function memoryStorage(): Storage {
  const values = new Map<string, string>()
  return {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => void values.set(key, value),
    removeItem: (key: string) => void values.delete(key),
    clear: () => values.clear(),
    key: () => null,
    get length() { return values.size },
  } as Storage
}

test('choosingStoresIdUnderKey', () => {
  const storage = memoryStorage()
  createQuemSouEuStore(storage).getState().choose(5)
  const saved = JSON.parse(storage.getItem(QUEM_SOU_EU_KEY) ?? 'null')
  assert.equal(saved.state.profileId, 5)
})

test('storeRestoresPersistedId', async () => {
  const storage = memoryStorage()
  createQuemSouEuStore(storage).getState().choose(7)
  const reopened = createQuemSouEuStore(storage)
  await reopened.persist.rehydrate()
  assert.equal(reopened.getState().profileId, 7)
})

test('changingReplacesSameKey', () => {
  const storage = memoryStorage()
  const store = createQuemSouEuStore(storage)
  store.getState().choose(1)
  store.getState().choose(2)
  assert.equal(store.getState().profileId, 2)
  assert.equal(JSON.parse(storage.getItem(QUEM_SOU_EU_KEY) ?? 'null').state.profileId, 2)
  assert.equal(storage.length, 1)
})

test('unknownIdIsNotSelected', () => {
  const profiles = [{ id: 1, name: 'Maria' }, { id: 2, name: 'João' }]
  assert.equal(resolveSelection(null, profiles), null)
  assert.equal(resolveSelection(9, profiles), null)
  assert.deepEqual(resolveSelection(2, profiles), { id: 2, name: 'João' })
})
