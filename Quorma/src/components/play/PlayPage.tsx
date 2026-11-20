import { useState, type CSSProperties } from 'react'
import { factions } from '../../data/factions'
import type { Page } from '../../data/navItems'
import { playModes } from '../../data/play'
import { resultLabel } from '../../data/profile'
import { ApiError, GENERIC } from '../../lib/api'
import { isDemoSession } from '../../lib/auth'
import { rememberSeries } from '../../lib/matches'
import { playMatchmaking, seriesScore } from '../../lib/series'
import type { Deck, FactionId, PlayStatus } from '../../types/deck'
import { playBlockReason } from '../../hooks/usePlayStatus'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import '../../styles/play.css'

// Titolo numerato di un passo ("1 · Scegli la modalità").
function Step({ num, id, title, extra }: { num: string; id: string; title: string; extra?: React.ReactNode }) {
  return (
    <div className="q-marker">
      <span className="q-marker__num">
        <span>{num}</span>
      </span>
      <h2 id={id} className="q-marker__title">{title}</h2>
      <span className="q-marker__line" />
      {extra}
    </div>
  )
}

const Star = () => (
  <svg className="q-stella" aria-hidden="true" viewBox="0 0 20 28">
    <path d="M10 0l2.2 11.6L20 14l-7.8 2.4L10 28l-2.2-11.6L0 14l7.8-2.4z" fill="#8FD6DB" />
  </svg>
)

const FACTION_IDS: FactionId[] = ['artificiali', 'umani']

// Errori dopo i quali la lista dei mazzi o "posso giocare?" non sono più aggiornati.
const STALE_DECKS = new Set(['deck.outdated', 'deck.defense_required', 'deck.not_found'])

type Played = { result: keyof typeof resultLabel; won: number; lost: number; opponent: string }

type Props = {
  decks: Deck[]
  playStatus: PlayStatus | null
  onNavigate: (page: Page) => void
  onDecksChanged: () => void // rilegge mazzi e "posso giocare?"
}

// Pagina Gioca: 1) modalità, 2) un mazzo per fazione, 3) la fazione della prima partita, poi "Inizia partita".
// Una serie è al meglio delle 3 e le fazioni si scambiano a ogni partita, per questo serve un mazzo per fazione.
// Si gioca solo con mazzi non obsoleti e con un mazzo di difesa valido per entrambe le fazioni (GET /decks/play-status).
// "Partita libera" = POST /matchmaking: il server sceglie un avversario e gioca tutta la serie contro i suoi mazzi
// di difesa. La serie finisce nello storico del backend (profilo); il browser ricorda turni e nomi dei mazzi (lib/matches.ts).
export default function PlayPage({ decks, playStatus, onNavigate, onDecksChanged }: Props) {
  const [modeId, setModeId] = useState(playModes.find((m) => m.available)?.id ?? playModes[0].id)
  const [chosen, setChosen] = useState<Partial<Record<FactionId, string>>>({})
  const [start, setStart] = useState<FactionId>('artificiali')
  const [playing, setPlaying] = useState(false)
  const [error, setError] = useState('')
  const [last, setLast] = useState<Played | null>(null) // ultima serie giocata, per il messaggio

  const demo = isDemoSession()
  const blocked = playBlockReason(playStatus)
  const mode = playModes.find((m) => m.id === modeId) ?? playModes[0]

  // Il backend salva solo mazzi completi: un mazzo non si può usare solo se è diventato obsoleto.
  const usable = (d: Deck) => !d.outdated
  // Mazzo scelto per ogni fazione: quello cliccato, altrimenti il mazzo di difesa, altrimenti il primo usabile.
  const deckFor = (f: FactionId) => {
    const own = decks.filter((d) => d.faction === f && usable(d))
    return own.find((d) => d.id === chosen[f]) ?? own.find((d) => d.defense) ?? own[0] ?? null
  }
  const picked = { artificiali: deckFor('artificiali'), umani: deckFor('umani') }
  const missing = FACTION_IDS.filter((f) => !picked[f])

  const hint = demo
    ? 'In modalità demo non si gioca: serve il backend.'
    : !mode.available
      ? mode.hint
      : missing.length === 2
        ? decks.length
          ? 'I tuoi mazzi sono obsoleti: modificane uno per fazione per giocare.'
          : 'Crea un mazzo per ogni fazione per giocare.'
        : missing.length === 1
          ? `Ti serve un mazzo valido ${missing[0] === 'umani' ? 'della' : 'delle'} ${factions[missing[0]].name}.`
          : blocked || mode.hint
  const canStart = !demo && mode.available && missing.length === 0 && !blocked && !playing

  const play = async () => {
    const a = picked.artificiali
    const u = picked.umani
    if (!canStart || !a || !u) return
    setPlaying(true)
    setError('')
    setLast(null)
    try {
      const series = await playMatchmaking(start, { artificiali: a.id, umani: u.id })
      rememberSeries(series, { artificiali: a.name, umani: u.name }) // la serie è già nello storico del backend: qui solo turni e nomi dei mazzi
      setLast(seriesScore(series))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : GENERIC)
      if (err instanceof ApiError && err.code && STALE_DECKS.has(err.code)) onDecksChanged()
    } finally {
      setPlaying(false)
    }
  }

  const summary = mode.title

  return (
    <main className="page play-page">
      <CurrencyBar />
      <PageTitle title="GIOCA" subtitle="SCEGLI MODALITÀ E MAZZI" />

      <section className="play-step" aria-labelledby="g-mod">
        <Step num="1" id="g-mod" title="Scegli la modalità" />
        <div role="radiogroup" aria-labelledby="g-mod" className="play-modi">
          {playModes.map((m) => {
            const on = m.id === modeId
            return (
              <button
                key={m.id}
                type="button"
                role="radio"
                aria-checked={on}
                aria-disabled={!m.available}
                title={m.available ? undefined : 'In arrivo'}
                className={m.available ? 'pick' : 'pick pick--off'}
                onClick={() => m.available && setModeId(m.id)}
              >
                <span className={`q-tela q-tela--arco play-modo ${on ? 'q-acceso' : ''}`}>
                  <span className="q-tela__in">
                    <span className="play-modo__testa">
                      <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
                        {m.paths.map((d) => (
                          <path key={d} d={d} />
                        ))}
                      </svg>
                      <span className="play-modo__titolo">{m.title}</span>
                      <span className="play-modo__linea" aria-hidden="true" />
                    </span>
                    <span className="play-modo__desc">{m.desc}</span>
                    <span className="play-modo__meta">{m.meta}</span>
                  </span>
                  <span className="q-borchia q-borchia--n" />
                </span>
              </button>
            )
          })}
        </div>
      </section>

      <section className="play-step" aria-labelledby="g-deck">
        <Step
          num="2"
          id="g-deck"
          title="Scegli i mazzi"
          extra={
            <button type="button" className="play-gestisci" onClick={() => onNavigate('mazzi')}>
              Gestisci
            </button>
          }
        />

        {decks.length === 0 ? (
          <div className="q-tela play-vuoto">
            <div className="q-tela__in">
              <p>Non hai ancora nessun mazzo.</p>
              <button type="button" className="q-btn q-btn--argento" onClick={() => onNavigate('nuovo-mazzo')}>
                <span className="q-btn__in">Crea mazzo</span>
              </button>
            </div>
          </div>
        ) : (
          <div className="play-fazioni">
            {FACTION_IDS.map((f) => {
              const faction = factions[f]
              const own = decks.filter((d) => d.faction === f)
              const groupId = `g-deck-${f}`
              return (
                <div key={f} className="play-fazione">
                  <h3 id={groupId} className="play-fazione__nome">{faction.name}</h3>
                  {own.length === 0 ? (
                    <p className="play-fazione__vuoto">
                      Nessun mazzo per questa fazione.{' '}
                      <button type="button" className="play-link" onClick={() => onNavigate('nuovo-mazzo')}>
                        Crea mazzo
                      </button>
                    </p>
                  ) : (
                    <div role="radiogroup" aria-labelledby={groupId} className="play-mazzi">
                      {own.map((d) => {
                        const ok = usable(d)
                        const on = d.id === picked[f]?.id
                        return (
                          <button
                            key={d.id}
                            type="button"
                            role="radio"
                            aria-checked={on}
                            aria-disabled={!ok}
                            className={ok ? 'pick' : 'pick pick--off'}
                            onClick={() => ok && setChosen((c) => ({ ...c, [f]: d.id }))}
                          >
                            <span className="q-mazzo">
                              <span className="q-mazzo__strato" aria-hidden="true" />
                              <span className="q-mazzo__strato" aria-hidden="true" />
                              <span className={`q-tela q-tela--arco q-carta ${on ? 'q-acceso' : ''}`}>
                                <span className="q-tela__in">
                                  <span className="q-carta__arte" style={{ backgroundImage: `url(${faction.cover})`, backgroundPosition: faction.coverFocus } as CSSProperties} />
                                  <span className="q-divisorio" />
                                  <span className="q-carta__targa">
                                    <span className="q-carta__nome">{d.name}</span>
                                    <span className={ok ? 'play-conta' : 'play-conta play-conta--off'}>
                                      {ok ? `${d.cards} / ${d.maxCards} carte${d.defense ? ' · Difesa' : ''}` : `${d.cards} / ${d.maxCards} · Obsoleto`}
                                    </span>
                                  </span>
                                </span>
                                <span className="q-borchia q-borchia--n" />
                              </span>
                            </span>
                          </button>
                        )
                      })}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </section>

      <div className="play-azione">
        <span className="q-divisorio" />
        <div className="play-azione__riga">
          <div className="play-riepilogo" aria-live="polite">
            <span>{summary}</span>
            {playing ? (
              <small>Partita in corso: il motore sta giocando la serie…</small>
            ) : error ? (
              <small className="play-errore" role="alert">{error}</small>
            ) : last ? (
              <small role="status">
                {resultLabel[last.result]} {last.won}–{last.lost} contro {last.opponent}.{' '}
                <button type="button" className="play-link" onClick={() => onNavigate('profilo')}>
                  Vedi nel profilo
                </button>
              </small>
            ) : (
              <small>{hint}</small>
            )}
          </div>
          <div className="play-bottoni">
            {/* Fazione della prima partita: nella seconda le fazioni si scambiano */}
            <div className="play-inizio">
              <span id="g-start" className="play-inizio__etichetta">Prima partita con</span>
              <div role="radiogroup" aria-labelledby="g-start" className="play-inizio__scelte">
                {FACTION_IDS.map((f) => (
                  <button key={f} type="button" role="radio" aria-checked={start === f} onClick={() => setStart(f)}>
                    {factions[f].name}
                  </button>
                ))}
              </div>
            </div>
            <button type="button" className="q-btn q-btn--acciaio play-gestisci-btn" onClick={() => onNavigate('mazzi')}>
              <span className="q-btn__in">Gestisci mazzi</span>
            </button>
            <button type="button" className="q-btn q-btn--sigillo" disabled={!canStart} aria-busy={playing} onClick={play}>
              <span className="q-btn__in">
                <Star />
                {playing ? 'In corso…' : 'Inizia partita'}
              </span>
              <span className="q-borchia q-borchia--w" />
              <span className="q-borchia q-borchia--e" />
            </button>
          </div>
        </div>
      </div>
    </main>
  )
}
