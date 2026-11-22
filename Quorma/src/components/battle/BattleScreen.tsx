import { useCallback, useEffect, useRef, useState, type CSSProperties } from 'react'
import { buildBattle, type BattleGame } from '../../lib/battle'
import { NO_NAMES, loadBattleNames } from '../../lib/battleNames'
import { getReplay, type SeriesResponse } from '../../lib/series'
import BattleBoard, { Story } from './BattleBoard'
import BattleResult from './BattleResult'
import '../../styles/battle.css'

type Props = {
  series: SeriesResponse
  startGame?: number // replay: la partita da cui partire (le altre restano nel punteggio e nel risultato)
  onClose: () => void
  onOpenMatches: () => void
}

type Phase = 'partita' | 'intermezzo' | 'esito'

const SPEEDS = [1, 2, 4]
const SPEED_KEY = 'quorma.battaglia.velocita'
const INTERLUDE_MS = 3600
const IMPACT_AT = 0.52 // il punto della battuta in cui il colpo arriva (battle.css, bt-colpo)
const CHRONICLE_MAX = 40

function readSpeed() {
  try {
    const n = Number(localStorage.getItem(SPEED_KEY))
    return SPEEDS.includes(n) ? n : 1
  } catch {
    return 1
  }
}

const NextIcon = ({ double }: { double?: boolean }) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {double ? <path d="M3 6l7 6-7 6zM11 6l7 6-7 6zM21 5v14" /> : <path d="M6 6l9 6-9 6zM19 5v14" />}
  </svg>
)

// La battaglia a tutto schermo: anima dal log del motore una serie già giocata. Le partite si susseguono da sole;
// in alto a destra ci sono la velocità e i due salti. Esc porta al risultato e, dal risultato, chiude.
// Una serie senza log (la stanza giocata mentre aspettavo) li richiede al replay; se non arrivano si va al risultato.
export default function BattleScreen({ series, startGame, onClose, onOpenMatches }: Props) {
  const dialog = useRef<HTMLDialogElement>(null)
  const [games, setGames] = useState<BattleGame[] | null>(null)
  const [phase, setPhase] = useState<Phase>('partita')
  const [gameIndex, setGameIndex] = useState(0)
  const [beatIndex, setBeatIndex] = useState(0)
  const [impactKey, setImpactKey] = useState('')
  const [speed, setSpeed] = useState(readSpeed)
  const me = series.mySeat

  useEffect(() => {
    const el = dialog.current
    if (el && !el.open) el.showModal()
    return () => el?.close()
  }, [])

  useEffect(() => {
    let current = true
    const ordered = [...series.games].sort((a, b) => a.gameNumber - b.gameNumber)
    Promise.all([
      loadBattleNames().catch(() => NO_NAMES), // senza nomi le carte restano riconoscibili da rango e seme
      Promise.allSettled(ordered.map(async (g) => g.log ?? (await getReplay(series.id, g.gameNumber)).log)),
    ]).then(([names, logs]) => {
      if (!current) return
      const built = logs.flatMap((r, i) => {
        if (r.status !== 'fulfilled') return []
        try {
          return [buildBattle(r.value, me, ordered[i].gameNumber, names)]
        } catch {
          return [] // log inatteso: la partita resta visibile solo nel risultato
        }
      })
      setGames(built)
      setGameIndex(Math.max(0, built.findIndex((g) => g.n === startGame)))
      if (!built.length) setPhase('esito')
    })
    return () => {
      current = false
    }
  }, [series, me, startGame])

  const game = games?.[gameIndex] ?? null
  const beat = game?.beats[beatIndex] ?? null
  const key = `${gameIndex}:${beatIndex}`
  const lastGame = !games || gameIndex >= games.length - 1

  const endGame = () => setPhase(lastGame ? 'esito' : 'intermezzo')
  const nextGame = useCallback(() => {
    setGameIndex((i) => i + 1)
    setBeatIndex(0)
    setPhase('partita')
  }, [])

  // L'orologio della battaglia: a metà di ogni colpo c'è l'impatto, alla fine si passa alla battuta successiva.
  useEffect(() => {
    if (phase !== 'partita' || !game || !beat) return
    const hasStrikes = beat.strikes.length > 0
    const impact = hasStrikes ? window.setTimeout(() => setImpactKey(key), (beat.ms * IMPACT_AT) / speed) : 0
    const next = window.setTimeout(() => {
      if (beatIndex < game.beats.length - 1) setBeatIndex(beatIndex + 1)
      else setPhase(gameIndex < (games?.length ?? 0) - 1 ? 'intermezzo' : 'esito')
    }, beat.ms / speed)
    return () => {
      window.clearTimeout(impact)
      window.clearTimeout(next)
    }
  }, [phase, game, beat, beatIndex, gameIndex, games, key, speed])

  useEffect(() => {
    if (phase !== 'intermezzo') return
    const t = window.setTimeout(nextGame, INTERLUDE_MS / speed)
    return () => window.clearTimeout(t)
  }, [phase, speed, nextGame])

  const changeSpeed = (s: number) => {
    setSpeed(s)
    try {
      localStorage.setItem(SPEED_KEY, String(s))
    } catch {
      /* la velocità vale solo per questa battaglia */
    }
  }

  const impacted = !beat?.strikes.length || impactKey === key
  const shownBoard = beat && game ? (impacted || beatIndex === 0 ? beat.board : game.beats[beatIndex - 1].board) : null

  // Punteggio della serie fin qui: contano le partite già finite.
  const done = (games ?? []).slice(0, phase === 'partita' ? gameIndex : gameIndex + 1)
  const won = done.filter((g) => g.stats.result === 'vittoria').length
  const lost = done.filter((g) => g.stats.result === 'sconfitta').length
  const opponent = (me === 'A' ? series.playerB : series.playerA).username
  const myName = (me === 'A' ? series.playerA : series.playerB).username
  const justPlayed = phase === 'intermezzo' ? game : null

  return (
    <dialog
      ref={dialog}
      className="bt"
      aria-label={`Battaglia contro ${opponent}`}
      style={{ '--t': `${(beat?.ms ?? 900) / speed}ms`, '--v': speed } as CSSProperties}
      onCancel={(e) => {
        e.preventDefault()
        if (phase === 'esito') onClose()
        else setPhase('esito')
      }}
    >
      {phase !== 'esito' && (
        <header className="bt-testa">
          <div className="bt-serie">
            <strong>Partita {game?.n ?? 1} di {series.games.length}</strong>
            <span>{won}–{lost} contro {opponent}</span>
          </div>
          <div className="bt-comandi">
            <div className="bt-velocita" role="group" aria-label="Velocità della battaglia">
              {SPEEDS.map((s) => (
                <button key={s} type="button" aria-pressed={speed === s} onClick={() => changeSpeed(s)}>×{s}</button>
              ))}
            </div>
            {!lastGame && (
              <button type="button" className="q-btn q-btn--acciaio bt-salta" aria-label="Salta la partita" onClick={phase === 'intermezzo' ? nextGame : endGame}>
                <span className="q-btn__in"><NextIcon /><span>Salta partita</span></span>
              </button>
            )}
            <button type="button" className="q-btn q-btn--acciaio bt-salta" aria-label="Salta al risultato" onClick={() => setPhase('esito')}>
              <span className="q-btn__in"><NextIcon double /><span>Risultato</span></span>
            </button>
          </div>
        </header>
      )}

      {phase === 'esito' ? (
        <BattleResult
          series={series}
          games={games ?? []}
          onClose={onClose}
          onOpenMatches={() => {
            onClose()
            onOpenMatches()
          }}
        />
      ) : !games || !game || !beat || !shownBoard ? (
        <div className="bt-attesa" role="status"><span className="bt-rombo" aria-hidden="true" />Preparo la battaglia…</div>
      ) : (
        <>
          <div className="bt-palco">
            <BattleBoard
              game={game}
              beat={beat}
              beatIndex={beatIndex}
              board={shownBoard}
              impacted={impacted}
              names={{ me: myName, them: opponent }}
            />
            {/* La cronaca: quello che è appena successo, dal più recente. Su schermi stretti resta solo la frase al centro */}
            <aside className="bt-cronaca" aria-label="Cronaca della partita">
              <h2>Cronaca</h2>
              <ol>
                {game.beats.slice(1, beatIndex + 1).slice(-CHRONICLE_MAX).reverse().map((b, i, list) => (
                  <li key={`${b.turn}-${list.length - i}`} className={b.tags.length ? 'bt-cronaca__speciale' : undefined}>
                    {list[i + 1]?.turn !== b.turn && <span className="bt-cronaca__turno">Turno {b.turn}</span>}
                    <p><Story parts={b.story} me={game.me} /></p>
                    {b.tags.length > 0 && <small>{b.tags.join(' · ')}</small>}
                  </li>
                ))}
              </ol>
            </aside>
          </div>
          {justPlayed && (
            <div className={`bt-intermezzo bt-intermezzo--${justPlayed.stats.result}`} role="status">
              <span>Partita {justPlayed.n}</span>
              <strong>{justPlayed.stats.result === 'vittoria' ? 'Vinta' : justPlayed.stats.result === 'sconfitta' ? 'Persa' : 'Pareggiata'}</strong>
              <p>Serie {won}–{lost}</p>
            </div>
          )}
        </>
      )}
    </dialog>
  )
}
