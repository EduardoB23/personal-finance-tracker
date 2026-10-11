export type Tab = 'resumo' | 'lancar' | 'extrato' | 'cadastro' | 'configuracoes' | 'orcamentos' | 'metas'

const TABS: { id: Tab; label: string }[] = [
  { id: 'resumo', label: 'Resumo' },
  { id: 'lancar', label: 'Lançar' },
  { id: 'extrato', label: 'Extrato' },
]

export function BottomNav({ tab, onChange }: { tab: Tab; onChange: (tab: Tab) => void }) {
  return (
    <nav className="fixed inset-x-0 bottom-0 border-t border-stone-300 bg-white">
      <ul className="mx-auto grid max-w-md grid-cols-3">
        {TABS.map((item) => {
          const active = item.id === tab
          return (
            <li key={item.id}>
              <button
                type="button"
                aria-current={active ? 'page' : undefined}
                onClick={() => onChange(item.id)}
                className={`w-full py-3 text-sm font-semibold ${active ? 'border-t-2 border-emerald-700 text-emerald-800' : 'border-t-2 border-transparent text-stone-500'}`}
              >
                {item.label}
              </button>
            </li>
          )
        })}
      </ul>
    </nav>
  )
}
