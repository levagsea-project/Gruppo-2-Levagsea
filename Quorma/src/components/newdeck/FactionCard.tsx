import type { Faction } from '../../data/factions'

type Props = { faction: Faction; selected: boolean; onSelect: () => void }

// Una delle due carte fazione del design "Pagina-Mazzi-Fazione": arte grande, nome, quante creature e magie,
// sottofazioni in una riga. Scelta = filo acceso e spunta in alto a destra.
export default function FactionCard({ faction, selected, onSelect }: Props) {
  return (
    <button type="button" className="newdeck-fazione" aria-pressed={selected} onClick={onSelect}>
      <span className={`q-tela q-carta ${selected ? 'q-acceso' : ''}`}>
        <span className="q-tela__in">
          <span className="q-carta__arte" style={{ backgroundImage: `url(${faction.cover})`, backgroundPosition: faction.coverFocus }} />
          <span className="q-divisorio" />
          <span className="q-carta__targa">
            <span className="q-carta__nome">{faction.name}</span>
            <span className="newdeck-fazione__meta">
              <b>{faction.creatures}</b> creature · <b>{faction.spells}</b> magie ed equipaggiamenti
            </span>
            <span className="newdeck-fazione__sotto">{faction.subfactions.map((s) => s.name).join(' · ')}</span>
          </span>
        </span>
      </span>
      <span className="newdeck-fazione__spunta" aria-hidden="true">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#081014" strokeWidth="3" strokeLinecap="round">
          <path d="M5 12.5 10 17l9-10" />
        </svg>
      </span>
    </button>
  )
}
