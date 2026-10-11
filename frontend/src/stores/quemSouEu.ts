import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

// "Quem sou eu" é uma escolha por aparelho (D20): fica só no localStorage, nunca no servidor
export const QUEM_SOU_EU_KEY = 'quem-sou-eu'

export type QuemSouEuState = {
  profileId: number | null
  choose: (profileId: number) => void
}

export function createQuemSouEuStore(storage: Storage) {
  return create<QuemSouEuState>()(
    persist(
      (set) => ({
        profileId: null,
        choose: (profileId) => set({ profileId }),
      }),
      { name: QUEM_SOU_EU_KEY, storage: createJSONStorage(() => storage) },
    ),
  )
}

// Só vale se o id ainda existe na lista de perfis (critério 10)
export function resolveSelection<T extends { id: number }>(profileId: number | null, profiles: T[]): T | null {
  if (profileId === null) return null
  return profiles.find((profile) => profile.id === profileId) ?? null
}

// Em navegadores sem localStorage (modo privado, por exemplo) a escolha dura só a sessão da aba
function browserStorage(): Storage {
  try {
    return window.localStorage
  } catch {
    const memory = new Map<string, string>()
    return {
      getItem: (key: string) => memory.get(key) ?? null,
      setItem: (key: string, value: string) => void memory.set(key, value),
      removeItem: (key: string) => void memory.delete(key),
      clear: () => memory.clear(),
      key: () => null,
      get length() {
        return memory.size
      },
    } as Storage
  }
}

export const useQuemSouEu = createQuemSouEuStore(browserStorage())
