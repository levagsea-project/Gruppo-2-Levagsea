import { useState } from 'react'
import { currentPlayer } from '../../data/player'
import { factions } from '../../data/factions'
import { formatWhen } from '../../lib/matches'
import type { Match } from '../../data/profile'
import ResultLabel from './ResultLabel'

// Pannello a destra dello storico: tutti i dettagli della partita scelta.
export default function MatchDetail({ match }: { match: Match }) {
  const [copied, setCopied] = useState(false)
  const player = currentPlayer()

  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(match.code)
      setCopied(true)
      setTimeout(() => setCopied(false), 1800)
    } catch {
      /* il browser ha negato l'accesso agli appunti: non facciamo nulla */
    }
  }

  return (
    <section className="q-tela pf-detail" aria-label="Dettaglio partita">
      <div className="q-tela__in">
        <span className="pf-label">Partita{match.simulated && <em className="pf-sim">Simulata</em>}</span>
        <div className="pf-detail-head">
          <ResultLabel result={match.result} className="pf-detail-result" />
          <time>{formatWhen(match.playedAt)}</time>
        </div>

        <div className="pf-versus">
          <div>
            <b>{player.name}</b>
            <span className="pf-link">{match.deck} · {factions[match.myFaction].name}</span>
            <small>Vita finale {match.lifeMine.toLocaleString('it-IT')}</small>
          </div>
          <em>Contro</em>
          <div className="is-right">
            <b>{match.opponent}</b>
            <span className="pf-link">{factions[match.oppFaction].name}</span>
            <small>Vita finale {match.lifeOpp.toLocaleString('it-IT')}</small>
          </div>
        </div>

        <div className="pf-detail-stats">
          <div><b>{match.turns}</b><span>Turni</span></div>
          <div><b>{match.best}</b><span>Mano migliore</span></div>
          <div><b>{match.mode}</b><span>Modalità</span></div>
        </div>

        <span className="pf-label">Turni</span>
        <div className="pf-turns">
          <table>
            <thead>
              <tr><th>Turno</th><th>Tua mano</th><th>Avversario</th></tr>
            </thead>
            <tbody>
              {match.turnHands.map(([mine, theirs], i) => (
                <tr key={i}>
                  <td>{i + 1}</td>
                  <td>{mine}</td>
                  <td>{theirs}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <p className="pf-note">Ogni partita è deterministica: con il codice si può rivedere esattamente com'è andata.</p>
        <div className="pf-code">
          <code>{match.code}</code>
          <button type="button" onClick={copyCode}>{copied ? 'Copiato' : 'Copia codice'}</button>
        </div>
        {/* Il replay non esiste ancora: il bottone per ora non fa nulla */}
        <button type="button" className="q-btn q-btn--argento">
          <span className="q-btn__in">Guarda il replay</span>
        </button>
      </div>
    </section>
  )
}
