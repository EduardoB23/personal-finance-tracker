export type Profile = { id: number; name: string }

export function QuemSouEu({ profiles, onChoose, onCadastrar }: {
  profiles: Profile[]
  onChoose: (profileId: number) => void
  onCadastrar: () => void
}) {
  return (
    <main className="mx-auto grid min-h-dvh max-w-md content-center gap-4 px-4">
      <h1 className="text-2xl font-semibold">Quem é você?</h1>
      {profiles.length === 0 ? (
        <div className="grid gap-3">
          <p className="text-stone-600">Cadastre um perfil para começar.</p>
          <button type="button" onClick={onCadastrar} className="rounded-lg bg-emerald-700 py-3 font-semibold text-white">
            Cadastrar perfil
          </button>
        </div>
      ) : (
        <ul className="grid gap-2">
          {profiles.map((profile) => (
            <li key={profile.id}>
              <button
                type="button"
                onClick={() => onChoose(profile.id)}
                className="w-full rounded-lg border border-stone-400 bg-white py-4 text-lg font-medium"
              >
                {profile.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}
