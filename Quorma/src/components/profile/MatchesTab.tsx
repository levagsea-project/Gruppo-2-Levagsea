import { useState } from 'react'
import type { Game, Result, Series } from '../../data/profile'
import MatchRow from './MatchRow'
import MatchDetail from './MatchDetail'

const filters: { label: string; result?: Result }[] = [
  { label: 'Tutte' },
  { label: 'Vittorie', result: 'vittoria' },
  { label: 'Sconfitte', result: 'sconfitta' },
  { label: 'Pareggi', result: 'pareggio' },
]

type Props = {
  ready: boolean
  series: Series[]
  onLoadDetail: (s: Series, g: Game) => Promise<void>
  onPlay: () => void
}

// Scheda "Partite": filtri + elenco delle serie a sinistra, dettaglio della serie scelta a destra.
export default function MatchesTab({ ready, series, onLoadDetail, onPlay }: Props) {
  const [filter, setFilter] = useState(0) // filtro attivo
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const visible = series.filter((s) => !filters[filter].result || s.result === filters[filter].result)
  const selected = visible.find((s) => s.id === selectedId) ?? visible[0] // se non scelta, la prima

  return (
    <div className="pf-body pf-matches">
      <div className="pf-list">
        <div className="pf-filters" role="group" aria-label="Filtra le serie">
          {filters.map((f, i) => (
            <button key={f.label} type="button" className="pf-tab" aria-pressed={i === filter} onClick={() => setFilter(i)}>
              {f.label}
            </button>
          ))}
        </div>
        <div className="pf-match-list">
          {ready && visible.length === 0 && (
            <p className="pf-empty">
              {series.length === 0 ? (
                <>
                  Non hai ancora giocato serie.{' '}
                  <button type="button" className="pf-link-btn" onClick={onPlay}>Gioca ora</button>
                </>
              ) : (
                'Nessuna serie con questo filtro.'
              )}
            </p>
          )}
          {visible.map((s) => (
            <MatchRow key={s.id} series={s} selected={s.id === selected?.id} onSelect={() => setSelectedId(s.id)} />
          ))}
        </div>
      </div>

      {selected ? (
        // key: cambiando serie il dettaglio riparte dalla partita 1
        <MatchDetail key={selected.id} series={selected} onLoadDetail={onLoadDetail} />
      ) : (
        <section className="q-tela pf-detail" aria-label="Dettaglio serie">
          <div className="q-tela__in">
            <span className="pf-label">Serie</span>
            <p className="pf-empty">Qui vedrai il dettaglio di ogni serie: le tre partite, i turni e le mani.</p>
          </div>
        </section>
      )}
    </div>
  )
}
