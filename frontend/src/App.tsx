import { useCallback, useEffect, useState } from 'react'
import { getJson, PinRequiredError } from './api.ts'
import { BottomNav, type Tab } from './components/BottomNav.tsx'
import { currentMonth } from './lib/finance.ts'
import { Cadastro } from './screens/Cadastro.tsx'
import { Extrato } from './screens/Extrato.tsx'
import { Lancar } from './screens/Lancar.tsx'
import { PinScreen } from './screens/PinScreen.tsx'
import { QuemSouEu, type Profile } from './screens/QuemSouEu.tsx'
import { Resumo } from './screens/Resumo.tsx'
import { resolveSelection, useQuemSouEu } from './stores/quemSouEu.ts'

function App() {
  const profileId = useQuemSouEu((state) => state.profileId)
  const choose = useQuemSouEu((state) => state.choose)
  const [profiles, setProfiles] = useState<Profile[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [pinRequired, setPinRequired] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [tab, setTab] = useState<Tab>('resumo')
  const [picking, setPicking] = useState(false)

  const requirePin = useCallback(() => setPinRequired(true), [])

  useEffect(() => {
    getJson<Profile[]>('/api/profiles')
      .then((loaded) => {
        setProfiles(loaded)
        setLoadError(null)
      })
      .catch((error: unknown) => {
        if (error instanceof PinRequiredError) setPinRequired(true)
        else setLoadError('Não foi possível carregar os perfis.')
      })
  }, [reloadKey])

  if (pinRequired) {
    return (
      <PinScreen
        onUnlocked={() => {
          setPinRequired(false)
          setReloadKey((key) => key + 1)
        }}
      />
    )
  }
  if (loadError) return <p role="alert" className="p-4 text-red-700">{loadError}</p>
  if (profiles === null) return <p className="p-4 text-stone-600">Carregando...</p>

  if (tab === 'cadastro') {
    return (
      <Cadastro
        onBack={() => setTab('resumo')}
        onSaved={() => setReloadKey((key) => key + 1)}
        onPinRequired={requirePin}
      />
    )
  }

  const selected = resolveSelection(profileId, profiles)
  if (!selected || picking) {
    return (
      <QuemSouEu
        profiles={profiles}
        onChoose={(id) => {
          choose(id)
          setPicking(false)
        }}
        onCadastrar={() => setTab('cadastro')}
      />
    )
  }

  return (
    <>
      {tab === 'resumo' && (
        <Resumo
          profileId={selected.id}
          personName={selected.name}
          currentMonth={currentMonth()}
          onTrocar={() => setPicking(true)}
          onCadastros={() => setTab('cadastro')}
          onPinRequired={requirePin}
        />
      )}
      {tab === 'lancar' && <Lancar profileId={selected.id} onPinRequired={requirePin} />}
      {tab === 'extrato' && <Extrato profiles={profiles} onPinRequired={requirePin} />}
      <BottomNav tab={tab} onChange={setTab} />
    </>
  )
}

export default App
