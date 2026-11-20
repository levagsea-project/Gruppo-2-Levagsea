import type { Overview } from '../../data/profile'
import { factions } from '../../data/factions'

// Colonna centrale della panoramica. Tutti i numeri arrivano da `overview`, calcolato dallo storico (lib/stats.ts).
// Vittorie e sconfitte contano le serie (al meglio delle 3); le mani contano i turni delle partite di cui
// conosciamo il log (giocate da questo browser o aperte nel dettaglio).
export default function StatsPanel({ ready, overview }: { ready: boolean; overview: Overview }) {
  const { total, wins, losses, draws, detailed, hands, bestHand, faction } = overview
  const numbers = [
    { label: 'Serie', value: total },
    { label: 'Vittorie', value: wins, extra: total ? `${Math.round((wins / total) * 100)}%` : undefined },
    { label: 'Sconfitte', value: losses },
    { label: 'Pareggi', value: draws },
  ]

  return (
    <section className="pf-stats" aria-label="Statistiche" aria-busy={!ready}>
      <h2 className="pf-heading">Statistiche</h2>
      {ready && total === 0 && <p className="pf-soon">Gioca la tua prima serie: qui compariranno le statistiche.</p>}

      <div className="pf-numbers">
        {numbers.map((n) => (
          <div key={n.label}>
            <b>{ready ? n.value : '—'}</b>
            <span>{n.label}</span>
            {n.extra && <small>{n.extra}</small>}
          </div>
        ))}
      </div>

      <span className="pf-label">Mani più frequenti</span>
      {hands.length === 0 ? (
        <p className="pf-empty pf-nohands">
          {total > 0 ? 'Apri una partita nella scheda Partite per leggerne le mani.' : 'Nessuna mano ancora.'}
        </p>
      ) : (
        <>
          <ul className="pf-bars">
            {hands.map((h) => (
              <li key={h.name}>
                <span>{h.name}</span>
                <span className="pf-bar"><span style={{ width: `${h.percent}%` }} /></span>
                <span className="pf-pct">{h.percent}%</span>
              </li>
            ))}
          </ul>
          <p className="pf-nota-mani">Su {detailed} {detailed === 1 ? 'partita' : 'partite'} di cui conosco i turni.</p>
        </>
      )}

      <div className="pf-highlights">
        <div>
          <span className="pf-label">Mano migliore</span>
          <strong>{bestHand?.name ?? '—'}</strong>
          <small>{bestHand ? `Ottenuta ${bestHand.times} ${bestHand.times === 1 ? 'volta' : 'volte'}` : 'Nessuna mano ancora'}</small>
        </div>
        <div>
          <span className="pf-label">Fazione più vincente</span>
          <strong>{faction ? factions[faction.id].name : '—'}</strong>
          <small>{faction ? `${faction.percent}% di vittorie su ${faction.games} ${faction.games === 1 ? 'partita' : 'partite'}` : 'Nessuna partita'}</small>
        </div>
      </div>
    </section>
  )
}
