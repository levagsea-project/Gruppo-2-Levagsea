import type { Overview } from '../../data/profile'
import { factions } from '../../data/factions'

// Colonna centrale della panoramica. Tutti i numeri arrivano da `overview`, calcolato dalle partite giocate.
export default function StatsPanel({ overview }: { overview: Overview }) {
  const { total, wins, losses, draws, hands, bestHand, faction } = overview
  const numbers = [
    { label: 'Partite', value: total },
    { label: 'Vittorie', value: wins, extra: total ? `${Math.round((wins / total) * 100)}%` : undefined },
    { label: 'Sconfitte', value: losses },
    { label: 'Pareggi', value: draws },
  ]

  return (
    <section className="pf-stats" aria-label="Statistiche">
      <h2 className="pf-heading">Statistiche</h2>
      {total === 0 && <p className="pf-soon">Gioca la tua prima partita: qui compariranno le statistiche.</p>}

      <div className="pf-numbers">
        {numbers.map((n) => (
          <div key={n.label}>
            <b>{n.value}</b>
            <span>{n.label}</span>
            {n.extra && <small>{n.extra}</small>}
          </div>
        ))}
      </div>

      <span className="pf-label">Mani giocate più spesso</span>
      {hands.length === 0 ? (
        <p className="pf-empty pf-nohands">Nessuna mano ancora.</p>
      ) : (
        <ul className="pf-bars">
          {hands.map((h) => (
            <li key={h.name}>
              <span>{h.name}</span>
              <span className="pf-bar"><span style={{ width: `${h.percent}%` }} /></span>
              <span className="pf-pct">{h.percent}%</span>
            </li>
          ))}
        </ul>
      )}

      <div className="pf-highlights">
        <div>
          <span className="pf-label">Mano migliore</span>
          <strong>{bestHand?.name ?? '—'}</strong>
          <small>{bestHand ? `Ottenuta ${bestHand.times} ${bestHand.times === 1 ? 'volta' : 'volte'}` : 'Nessuna partita'}</small>
        </div>
        <div>
          <span className="pf-label">Fazione più giocata</span>
          <strong>{faction ? factions[faction.id].name : '—'}</strong>
          <small>{faction ? `${faction.percent}% delle partite · ${faction.deck}` : 'Nessuna partita'}</small>
        </div>
      </div>
    </section>
  )
}
