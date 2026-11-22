export type Tab = 'panoramica' | 'partite' | 'impostazioni'
export const tabs: { id: Tab; label: string; subtitle: string }[] = [
  { id: 'panoramica', label: 'Panoramica', subtitle: '' }, // il sottotitolo è il nome del giocatore
  { id: 'partite', label: 'Partite', subtitle: 'STORICO DELLE PARTITE' },
  { id: 'impostazioni', label: 'Impostazioni', subtitle: 'IMPOSTAZIONI' },
]

type Props = { current: Tab; onChange: (tab: Tab) => void }

// Le tre schede in alto: Panoramica, Partite, Impostazioni.
export default function ProfileTabs({ current, onChange }: Props) {
  return (
    <div className="pf-tabs" role="tablist" aria-label="Sezioni del profilo">
      {tabs.map((t) => (
        <button
          key={t.id}
          type="button"
          role="tab"
          className="pf-tab"
          aria-selected={t.id === current}
          onClick={() => onChange(t.id)}
        >
          {t.label}
        </button>
      ))}
    </div>
  )
}
