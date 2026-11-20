import { useEffect, useState } from 'react'
import { currentPlayer } from '../../data/player'
import { factions } from '../../data/factions'
import { resultLabel } from '../../data/profile'
import type { Game, Series } from '../../data/profile'
import { ApiError, GENERIC } from '../../lib/api'
import { formatWhen } from '../../lib/matches'
import ResultLabel from './ResultLabel'

type Props = { series: Series; onLoadDetail: (s: Series, g: Game) => Promise<void> }

// Pannello a destra dello storico: la serie scelta (esito, punteggio, avversario) e le sue partite, una alla volta.
// Turni e mani di una partita vengono dal suo log: se il browser non lo conosce, lo chiede al replay
// (il motore rigioca la partita, qualche istante) e da lì in poi lo ricorda.
export default function MatchDetail({ series, onLoadDetail }: Props) {
  const [n, setN] = useState(series.games[0]?.n ?? 1)
  const [copied, setCopied] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)
  const player = currentPlayer()
  const game = series.games.find((g) => g.n === n) ?? series.games[0]
  const missing = !!game && !game.detail

  useEffect(() => {
    if (!game || !missing) return
    let current = true
    setLoading(true)
    setError('')
    onLoadDetail(series, game)
      .catch((err) => current && setError(err instanceof ApiError ? err.message : GENERIC))
      .finally(() => current && setLoading(false))
    return () => {
      current = false
    }
    // series e game cambiano a ogni lettura del dettaglio: bastano gli id e "manca il dettaglio"
  }, [series.id, game?.matchId, missing, attempt])

  const code = series.code ?? series.id
  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(code)
      setCopied(true)
      setTimeout(() => setCopied(false), 1800)
    } catch {
      /* il browser ha negato l'accesso agli appunti: non facciamo nulla */
    }
  }

  const detail = game?.detail

  return (
    <section className="q-tela pf-detail" aria-label="Dettaglio serie">
      <div className="q-tela__in">
        <span className="pf-label">Serie · {series.mode}</span>
        <div className="pf-detail-head">
          <span className="pf-detail-esito">
            <ResultLabel result={series.result} className="pf-detail-result" />
            <span className="pf-punteggio" aria-label={`${series.won} a ${series.lost}`}>{series.won}–{series.lost}</span>
          </span>
          <time>{formatWhen(series.finishedAt)}</time>
        </div>

        <div className="pf-versus">
          <div>
            <b>{player.name}</b>
          </div>
          <em>Contro</em>
          <div className="is-right">
            <b>{series.opponent}</b>
          </div>
        </div>

        {/* Le partite della serie: una scheda per partita, con il suo esito */}
        <div className="pf-filters pf-partite" role="group" aria-label="Partite della serie">
          {series.games.map((g) => (
            <button key={g.n} type="button" className="pf-tab pf-partita" aria-pressed={g.n === game?.n} onClick={() => setN(g.n)}>
              Partita {g.n}
              <small className={`is-${g.result}`}>{resultLabel[g.result]}</small>
            </button>
          ))}
        </div>

        {game && (
          <>
            <p className="pf-partita-fazioni">
              {factions[game.myFaction].name}
              {game.deck && <span> · {game.deck}</span>}
              <em> contro </em>
              {factions[game.oppFaction].name}
            </p>

            {detail ? (
              <>
                <div className="pf-detail-stats">
                  <div><b>{detail.turns}</b><span>Turni</span></div>
                  <div><b>{detail.best}</b><span>Mano migliore</span></div>
                  <div>
                    <b>{detail.lifeMine.toLocaleString('it-IT')} / {detail.lifeOpp.toLocaleString('it-IT')}</b>
                    <span>Vita finale tua / sua</span>
                  </div>
                </div>

                <span className="pf-label">Turni</span>
                <div className="pf-turns">
                  <table>
                    <thead>
                      <tr><th>Turno</th><th>Tua mano</th><th>Avversario</th></tr>
                    </thead>
                    <tbody>
                      {detail.turnHands.map(([mine, theirs], i) => (
                        <tr key={i}>
                          <td>{i + 1}</td>
                          <td>{mine}</td>
                          <td>{theirs}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            ) : (
              <div className="pf-turns pf-turns--vuoto" aria-live="polite">
                {error ? (
                  <p className="pf-avviso pf-avviso--errore" role="alert">
                    {error}{' '}
                    <button type="button" onClick={() => setAttempt((a) => a + 1)}>Riprova</button>
                  </p>
                ) : (
                  loading && <p className="pf-avviso">Il motore rigioca la partita per leggerne i turni…</p>
                )}
              </div>
            )}
          </>
        )}

        <p className="pf-note">Ogni partita è deterministica: con il codice della serie si può rivedere esattamente com'è andata.</p>
        <div className="pf-code">
          <code>{code}</code>
          <button type="button" onClick={copyCode}>{copied ? 'Copiato' : 'Copia codice'}</button>
        </div>
        {/* Il replay animato non esiste ancora */}
        <button type="button" className="q-btn q-btn--argento" disabled title="In arrivo">
          <span className="q-btn__in">Guarda il replay · in arrivo</span>
        </button>
      </div>
    </section>
  )
}
