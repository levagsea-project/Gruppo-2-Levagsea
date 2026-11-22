import { factions } from '../../data/factions'
import { resultLabel } from '../../data/profile'
import { fmt, type BattleGame, type SideStats } from '../../lib/battle'
import { seriesScore, type SeriesResponse } from '../../lib/series'
import CreatureCard from '../cards/CreatureCard'

type Props = {
  series: SeriesResponse
  games: BattleGame[]
  onClose: () => void
  onOpenMatches: () => void
}

const ROWS: [keyof SideStats, string][] = [
  ['damage', 'Danni alle creature'],
  ['spill', 'Danni alla vita'],
  ['kills', 'Creature distrutte'],
  ['lost', 'Creature perse'],
]

const sum = (games: BattleGame[], who: 'me' | 'them', key: keyof SideStats) => games.reduce((n, g) => n + g.stats[who][key], 0)

// Il risultato della serie: verdetto, partita per partita, confronto dei totali e la carta che ha inflitto più danni.
export default function BattleResult({ series, games, onClose, onOpenMatches }: Props) {
  const score = seriesScore(series)
  const mvp = games.flatMap((g) => (g.stats.mvp ? [g.stats.mvp] : [])).reduce<BattleGame['stats']['mvp']>((best, m) => (!best || m.damage > best.damage ? m : best), null)
  const turns = games.reduce((n, g) => n + g.stats.turns, 0)
  const missing = games.length < series.games.length

  return (
    <div className="bt-esito">
      <header className={`bt-verdetto bt-verdetto--${score.result}`}>
        <span className="bt-rombo" aria-hidden="true" />
        <h2>{resultLabel[score.result]}</h2>
        <p><b>{score.won}–{score.lost}</b> contro {score.opponent}{turns > 0 && ` · ${turns} turni`}</p>
      </header>

      {missing && <p className="bt-nota" role="status">Il dettaglio di alcune partite non è disponibile adesso. Lo trovi nel profilo.</p>}

      {games.length > 0 && (
        <div className="bt-esito__corpo">
          <section className="bt-partite" aria-label="Partite della serie">
            {games.map((g) => (
              <article key={g.n} className={`q-tela q-tela--arco bt-partita bt-partita--${g.stats.result}`}>
                <div className="q-tela__in">
                  <h3>Partita {g.n}</h3>
                  <strong>{resultLabel[g.stats.result]}</strong>
                  <p>{g.stats.reason}</p>
                  <dl>
                    <div><dt>Con</dt><dd>{factions[g.factions[g.me]].name}</dd></div>
                    <div><dt>Turni</dt><dd>{g.stats.turns}</dd></div>
                    <div><dt>Vita finale</dt><dd>{fmt(g.stats.lifeMe)} – {fmt(g.stats.lifeThem)}</dd></div>
                    <div><dt>Mano migliore</dt><dd>{g.stats.best}</dd></div>
                  </dl>
                </div>
                <span className="q-borchia q-borchia--n" />
              </article>
            ))}
          </section>

          <section className="bt-totali" aria-labelledby="bt-totali">
            <h3 id="bt-totali">Statistiche della serie</h3>
            <table>
              <thead>
                <tr><th scope="col"><span className="sr-only">Statistica</span></th><th scope="col">Tu</th><th scope="col">{score.opponent}</th></tr>
              </thead>
              <tbody>
                {ROWS.map(([key, label]) => (
                  <tr key={key}>
                    <th scope="row">{label}</th>
                    <td>{fmt(sum(games, 'me', key))}</td>
                    <td>{fmt(sum(games, 'them', key))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {mvp && (
              <div className="bt-mvp">
                <div className="bt-mvp__carta"><CreatureCard card={mvp.card} sizes="96px" /></div>
                <div>
                  <h4>Carta migliore</h4>
                  <strong>{mvp.card.name}</strong>
                  <p>{fmt(mvp.damage)} danni · {mvp.kills} {mvp.kills === 1 ? 'creatura distrutta' : 'creature distrutte'}</p>
                </div>
              </div>
            )}
          </section>
        </div>
      )}

      <div className="bt-esito__azioni">
        <button type="button" className="q-btn q-btn--acciaio" onClick={onOpenMatches}>
          <span className="q-btn__in">Vedi nel profilo</span>
        </button>
        <button type="button" className="q-btn q-btn--argento" onClick={onClose} autoFocus>
          <span className="q-btn__in">Chiudi</span>
        </button>
      </div>
    </div>
  )
}
