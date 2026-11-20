import type { Match } from '../../data/profile'
import ResultLabel from './ResultLabel'

type Props = { matches: Match[]; onAll: () => void }

// Riquadro a destra: le ultime 5 partite + bottone per vederle tutte.
export default function RecentMatches({ matches, onAll }: Props) {
  return (
    <section className="q-tela pf-recent" aria-label="Ultime partite">
      <div className="q-tela__in">
        <span className="pf-label">Ultime partite</span>
        {matches.length === 0 ? (
          <p className="pf-empty pf-recent-list">Nessuna partita ancora. Quelle che giocherai compariranno qui.</p>
        ) : (
          <ul className="pf-recent-list">
            {matches.slice(0, 5).map((m) => (
              <li key={m.id}>
                <ResultLabel result={m.result} />
                <span>
                  <b>contro {m.opponent}</b>
                  <small>{m.mode} · {m.when}</small>
                </span>
              </li>
            ))}
          </ul>
        )}
        <button type="button" className="q-btn q-btn--acciaio" onClick={onAll}>
          <span className="q-btn__in">Tutte le partite</span>
        </button>
      </div>
    </section>
  )
}
