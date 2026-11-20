import type { Series } from '../../data/profile'
import { formatWhen } from '../../lib/matches'
import ResultLabel from './ResultLabel'

type Props = { ready: boolean; series: Series[]; onAll: () => void }

// Riquadro a destra: le ultime 5 serie + bottone per vederle tutte.
export default function RecentMatches({ ready, series, onAll }: Props) {
  return (
    <section className="q-tela pf-recent" aria-label="Ultime serie">
      <div className="q-tela__in">
        <span className="pf-label">Ultime serie</span>
        {series.length === 0 ? (
          <p className="pf-empty pf-recent-list">{ready ? 'Nessuna serie ancora. Quelle che giocherai compariranno qui.' : ''}</p>
        ) : (
          <ul className="pf-recent-list">
            {series.slice(0, 5).map((s) => (
              <li key={s.id}>
                <ResultLabel result={s.result} />
                <span>
                  <b>contro {s.opponent}</b>
                  <small>{s.mode} · {s.won}–{s.lost} · {formatWhen(s.finishedAt)}</small>
                </span>
              </li>
            ))}
          </ul>
        )}
        <button type="button" className="q-btn q-btn--acciaio" onClick={onAll}>
          <span className="q-btn__in">Tutte le serie</span>
        </button>
      </div>
    </section>
  )
}
