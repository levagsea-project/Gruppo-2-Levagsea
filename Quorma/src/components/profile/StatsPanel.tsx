import { handNames, statLabels } from '../../data/profile'
import type { Overview } from '../../data/profile'

const EMPTY = '—' // dato non ancora disponibile

// Colonna centrale della panoramica: numeri, mani più giocate, mano migliore e fazione preferita.
// overview = null → il backend non ha ancora i dati: si vedono i trattini.
export default function StatsPanel({ overview }: { overview: Overview | null }) {
  return (
    <section className="pf-stats" aria-label="Statistiche">
      <h2 className="pf-heading">Statistiche</h2>
      {!overview && <p className="pf-soon">Le statistiche appariranno dopo le prime partite.</p>}

      <div className="pf-numbers">
        {statLabels.map((label, i) => (
          <div key={label}>
            <b>{overview?.stats[i].value ?? EMPTY}</b>
            <span>{label}</span>
            {overview?.stats[i].extra && <small>{overview.stats[i].extra}</small>}
          </div>
        ))}
      </div>

      <span className="pf-label">Mani giocate più spesso</span>
      <ul className="pf-bars">
        {handNames.map((name) => {
          const percent = overview?.hands.find((h) => h.name === name)?.percent
          return (
            <li key={name}>
              <span>{name}</span>
              <span className="pf-bar"><span style={{ width: `${percent ?? 0}%` }} /></span>
              <span className="pf-pct">{percent === undefined ? EMPTY : `${percent}%`}</span>
            </li>
          )
        })}
      </ul>

      <div className="pf-highlights">
        <div>
          <span className="pf-label">Mano migliore</span>
          <strong>{overview?.bestHand.name ?? EMPTY}</strong>
          <small>{overview?.bestHand.detail ?? 'In arrivo'}</small>
        </div>
        <div>
          <span className="pf-label">Fazione più giocata</span>
          <strong>{overview?.faction.name ?? EMPTY}</strong>
          <small>{overview?.faction.detail ?? 'In arrivo'}</small>
        </div>
      </div>
    </section>
  )
}
