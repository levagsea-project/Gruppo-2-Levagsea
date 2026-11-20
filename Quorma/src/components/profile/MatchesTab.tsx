import { useState } from 'react'
import type { Match, Result } from '../../data/profile'
import MatchRow from './MatchRow'
import MatchDetail from './MatchDetail'

const filters: { label: string; result?: Result }[] = [
  { label: 'Tutte' },
  { label: 'Vittorie', result: 'vittoria' },
  { label: 'Sconfitte', result: 'sconfitta' },
  { label: 'Pareggi', result: 'pareggio' },
]

// Scheda "Partite": filtri + elenco a sinistra, dettaglio della partita scelta a destra.
export default function MatchesTab({ matches }: { matches: Match[] }) {
  const [filter, setFilter] = useState(0) // filtro attivo
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const visible = matches.filter((m) => !filters[filter].result || m.result === filters[filter].result)
  const selected = visible.find((m) => m.id === selectedId) ?? visible[0] // se non scelta, la prima

  return (
    <div className="pf-body pf-matches">
      <div className="pf-list">
        <div className="pf-filters" role="group" aria-label="Filtra le partite">
          {filters.map((f, i) => (
            <button key={f.label} type="button" className="pf-tab" aria-pressed={i === filter} onClick={() => setFilter(i)}>
              {f.label}
            </button>
          ))}
        </div>
        <div className="pf-match-list">
          {visible.length === 0 && (
            <p className="pf-empty">{matches.length === 0 ? 'Non hai ancora giocato partite.' : 'Nessuna partita con questo filtro.'}</p>
          )}
          {visible.map((m) => (
            <MatchRow key={m.id} match={m} selected={m.id === selected?.id} onSelect={() => setSelectedId(m.id)} />
          ))}
        </div>
      </div>

      {selected ? (
        <MatchDetail match={selected} />
      ) : (
        <section className="q-tela pf-detail" aria-label="Dettaglio partita">
          <div className="q-tela__in">
            <span className="pf-label">Partita</span>
            <p className="pf-empty">Qui vedrai il dettaglio di ogni partita: turni, mani e codice per rivederla.</p>
          </div>
        </section>
      )}
    </div>
  )
}
