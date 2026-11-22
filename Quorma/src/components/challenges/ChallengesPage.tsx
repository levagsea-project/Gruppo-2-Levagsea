import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { factions } from '../../data/factions'
import type { Page } from '../../data/navItems'
import { resultLabel } from '../../data/profile'
import { playBlockReason } from '../../hooks/usePlayStatus'
import { ApiError, GENERIC } from '../../lib/api'
import { getSession, isDemoSession } from '../../lib/auth'
import { toFaction } from '../../lib/cards'
import {
  acceptChallenge, cancelChallenge, cancelRoom, createChallenge, declineChallenge, getMyRoom, joinRoom,
  listChallenges, listRooms, openRoom, timeLeft, type Challenge, type Room,
} from '../../lib/challenges'
import { rememberSeries } from '../../lib/matches'
import { onRadioKeyDown } from '../../lib/radioKeys'
import { seriesScore, type DeckPick, type SeriesResponse } from '../../lib/series'
import { prefersReducedMotion } from '../../lib/settings'
import type { Deck, FactionId, PlayStatus } from '../../types/deck'
import BattleScreen from '../battle/BattleScreen'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import '../../styles/collection.css'
import '../../styles/challenges.css'

type Tab = 'ricevute' | 'inviate' | 'stanze'
type Played = ReturnType<typeof seriesScore>

const FACTION_IDS: FactionId[] = ['artificiali', 'umani']
const POLL_MS = 10_000 // il backend non manda notifiche: stanze e sfide si rileggono a intervalli
const STATUS: Record<Challenge['status'], string> = {
  PENDING: 'In attesa', ACCEPTED: 'Giocata', DECLINED: 'Rifiutata', CANCELLED: 'Ritirata', EXPIRED: 'Scaduta',
}
const STALE_DECKS = new Set(['deck.outdated', 'deck.defense_required', 'deck.not_found'])

const Flag = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M6 21V3M6 4h12l-3 4.5 3 4.5H6" />
  </svg>
)

type Props = {
  decks: Deck[]
  playStatus: PlayStatus | null
  onNavigate: (page: Page) => void
  onOpenMatches: () => void
  onChanged: () => void // rilegge mazzi, "posso giocare?" e il segnale sul menu
}

// Sfide (design: Pagina-Sfide). Il design disegna missioni giornaliere e settimanali, che il backend non ha:
// la stessa struttura (schede, righe, riepilogo a destra) ospita ciò che esiste, sfide per nome e stanze.
export default function ChallengesPage({ decks, playStatus, onNavigate, onOpenMatches, onChanged }: Props) {
  const demo = isDemoSession()
  const me = getSession()?.username ?? ''
  const [tab, setTab] = useState<Tab>('ricevute')
  const [challenges, setChallenges] = useState<Challenge[] | null>(null)
  const [rooms, setRooms] = useState<Room[]>([])
  const [myRoom, setMyRoom] = useState<Room | null>(null)
  const [loadError, setLoadError] = useState('')
  const [busy, setBusy] = useState('') // id dell'azione in corso
  const [error, setError] = useState('')
  const [played, setPlayed] = useState<Played | null>(null)
  const [battle, setBattle] = useState<SeriesResponse | null>(null) // la serie da animare a tutto schermo
  const [opponent, setOpponent] = useState('')
  const [start, setStart] = useState<FactionId>('artificiali')
  const [chosen, setChosen] = useState<Partial<Record<FactionId, string>>>({})

  const load = useCallback(async () => {
    try {
      const [c, r, mine] = await Promise.all([listChallenges(), listRooms(), getMyRoom()])
      setChallenges(c)
      setRooms(r)
      setMyRoom(mine)
      setLoadError('')
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : GENERIC)
    }
  }, [])

  useEffect(() => {
    if (demo) return
    void load()
    const timer = window.setInterval(() => {
      if (!document.hidden) void load()
    }, POLL_MS)
    return () => window.clearInterval(timer)
  }, [demo, load])

  const usable = (f: FactionId) => decks.filter((d) => d.faction === f && !d.outdated)
  const deckFor = (f: FactionId) => {
    const own = usable(f)
    return own.find((d) => d.id === chosen[f]) ?? own.find((d) => d.defense) ?? own[0] ?? null
  }
  const a = deckFor('artificiali')
  const u = deckFor('umani')
  const pick: DeckPick | null = a && u ? { artificiali: a.id, umani: u.id } : null
  const blocked = playBlockReason(playStatus) || (pick ? '' : 'Ti serve un mazzo valido per ogni fazione.')
  const canAct = !demo && !blocked && !busy

  // Esegue un'azione, mostra l'eventuale errore e rilegge gli elenchi.
  const run = async (id: string, action: () => Promise<SeriesResponse | unknown>, isSeries = false) => {
    setBusy(id)
    setError('')
    setPlayed(null)
    try {
      const result = await action()
      if (isSeries && a && u) {
        const series = result as SeriesResponse
        rememberSeries(series, { artificiali: a.name, umani: u.name })
        setPlayed(seriesScore(series))
        setBattle(series)
      }
      onChanged()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : GENERIC)
      if (err instanceof ApiError && err.code && STALE_DECKS.has(err.code)) onChanged()
    } finally {
      setBusy('')
      void load()
    }
  }

  const openMyRoom = () => {
    if (!pick) return
    void run('stanza', async () => {
      await openRoom(start, pick)
      setTab('stanze')
    })
  }

  // Dallo stato vuoto al modulo: su telefono il modulo sta più in basso, quindi ci si porta anche lì.
  const goToChallengeForm = () => {
    const field = document.getElementById('sfida-nome')
    field?.scrollIntoView({ block: 'center', behavior: prefersReducedMotion() ? 'auto' : 'smooth' })
    field?.focus({ preventScroll: true })
  }

  const send = (e: FormEvent) => {
    e.preventDefault()
    const name = opponent.trim()
    if (!name || !pick || !canAct) return
    void run('nuova', async () => {
      await createChallenge(name, start, pick)
      setOpponent('')
      setTab('inviate')
    })
  }

  const list = challenges ?? []
  const received = list.filter((c) => c.opponent.username === me)
  const sent = list.filter((c) => c.challenger.username === me)
  const pendingReceived = received.filter((c) => c.status === 'PENDING').length
  const roomOpen = myRoom?.status === 'OPEN' ? myRoom : null
  const rows = tab === 'ricevute' ? received : sent
  const TABS: [Tab, string, number][] = [['ricevute', 'Ricevute', pendingReceived], ['inviate', 'Inviate', sent.filter((c) => c.status === 'PENDING').length], ['stanze', 'Stanze', rooms.length]]

  return (
    <main className="page sfide-page">
      <CurrencyBar />
      <PageTitle title="SFIDE" subtitle="SFIDA I GIOCATORI E APRI UNA STANZA" />

      <div className="sfide-corpo">
        <section className="sfide-lista" aria-label="Sfide e stanze">
          <div className="sfide-schede">
            <div className="coll-chips" role="radiogroup" aria-label="Tipo di sfida" onKeyDown={onRadioKeyDown}>
              {TABS.map(([id, label, n]) => (
                <button key={id} type="button" role="radio" className="m-chip" aria-checked={tab === id} tabIndex={tab === id ? 0 : -1} onClick={() => setTab(id)}>
                  {label} <span className="m-chip__n">{n}</span>
                </button>
              ))}
            </div>
            <span className="q-marker__line" />
          </div>

          {(error || played) && (
            <div className={`sfide-esito ${error ? 'sfide-esito--errore' : ''}`} role={error ? 'alert' : 'status'}>
              {error || (played && (
                <>
                  {resultLabel[played.result]} {played.won}–{played.lost} contro {played.opponent}.{' '}
                  <button type="button" className="play-link" onClick={onOpenMatches}>Vedi nel profilo</button>
                </>
              ))}
            </div>
          )}

          {demo ? (
            <p className="coll-vuoto">In modalità demo le sfide non sono disponibili: serve il backend.</p>
          ) : loadError && !challenges ? (
            <div className="coll-vuoto" role="alert">
              <p>{loadError}</p>
              <button type="button" className="q-btn q-btn--acciaio" onClick={() => void load()}><span className="q-btn__in">Riprova</span></button>
            </div>
          ) : !challenges ? (
            <p className="coll-vuoto" role="status">Caricamento delle sfide…</p>
          ) : tab === 'stanze' ? (
            <div className="sfide-righe">
              {roomOpen && (
                <article className="q-tela q-acceso">
                  <div className="q-tela__in x-sfida">
                    <span className="x-sigillo" aria-hidden="true"><Flag /></span>
                    <div>
                      <h3>La tua stanza</h3>
                      <p>Codice {roomOpen.code} · in attesa di un avversario · scade tra {timeLeft(roomOpen.expiresAt)}</p>
                    </div>
                    <div className="x-premio">
                      <button type="button" className="q-btn q-btn--acciaio sfide-btn" disabled={!!busy} onClick={() => void run(roomOpen.code, () => cancelRoom(roomOpen.code))}>
                        <span className="q-btn__in">Chiudi</span>
                      </button>
                    </div>
                  </div>
                </article>
              )}
              {/* /rooms/mine resta sull'ultima stanza: l'avviso vale solo finché la stanza sarebbe stata aperta */}
              {myRoom?.status === 'MATCHED' && Date.parse(myRoom.expiresAt) > Date.now() && (
                <p className="sfide-esito">
                  Qualcuno è entrato nella tua stanza e la serie è stata giocata.{' '}
                  <button type="button" className="play-link" onClick={onOpenMatches}>Vedi nel profilo</button>
                </p>
              )}
              {rooms.map((r) => (
                <article key={r.id} className="q-tela">
                  <div className="q-tela__in x-sfida">
                    <span className="x-sigillo" aria-hidden="true"><Flag /></span>
                    <div>
                      <h3>{r.creator.username}</h3>
                      <p>Gioca la prima partita con {factions[toFaction(r.creatorFaction)].name} · scade tra {timeLeft(r.expiresAt)}</p>
                    </div>
                    <div className="x-premio">
                      <button type="button" className="q-btn q-btn--argento sfide-btn" disabled={!canAct} aria-busy={busy === r.code} onClick={() => pick && void run(r.code, () => joinRoom(r.code, pick), true)}>
                        <span className="q-btn__in">{busy === r.code ? 'In corso…' : 'Entra'}</span>
                      </button>
                    </div>
                  </div>
                </article>
              ))}
              {rooms.length === 0 && !roomOpen && (
                <div className="coll-vuoto">
                  <p>{blocked ? `Nessuna stanza aperta. ${blocked}` : 'Nessuna stanza aperta: apri la tua e aspetta che entri qualcuno.'}</p>
                  {blocked ? (
                    <button type="button" className="q-btn q-btn--argento" onClick={() => onNavigate('mazzi')}>
                      <span className="q-btn__in">Gestisci mazzi</span>
                    </button>
                  ) : (
                    <button type="button" className="q-btn q-btn--argento" disabled={!canAct} aria-busy={busy === 'stanza'} onClick={openMyRoom}>
                      <span className="q-btn__in">{busy === 'stanza' ? 'Apertura…' : 'Apri una stanza'}</span>
                    </button>
                  )}
                </div>
              )}
            </div>
          ) : rows.length === 0 ? (
            <div className="coll-vuoto">
              <p>{tab === 'ricevute' ? 'Non hai ricevuto sfide. Puoi lanciarne una tu.' : 'Non hai inviato sfide.'}</p>
              <button type="button" className="q-btn q-btn--argento" onClick={goToChallengeForm}>
                <span className="q-btn__in">Sfida un giocatore</span>
              </button>
            </div>
          ) : (
            <div className="sfide-righe">
              {rows.map((c) => {
                const incoming = tab === 'ricevute'
                const other = incoming ? c.challenger.username : c.opponent.username
                const pending = c.status === 'PENDING'
                const theirFaction = incoming ? toFaction(c.challengerFaction) : toFaction(c.challengerFaction) === 'umani' ? 'artificiali' : 'umani'
                return (
                  <article key={c.id} className={`q-tela ${pending && incoming ? 'q-acceso' : ''} ${pending ? '' : 'x-riscossa-card'}`}>
                    <div className="q-tela__in x-sfida">
                      <span className="x-sigillo" aria-hidden="true"><Flag /></span>
                      <div>
                        <h3>{other}</h3>
                        <p>{other} inizia con {factions[theirFaction].name} · al meglio delle 3</p>
                        <span className="sfide-stato">{pending ? `Scade tra ${timeLeft(c.expiresAt)}` : STATUS[c.status]}</span>
                      </div>
                      <div className="x-premio">
                        {pending && incoming && (
                          <>
                            <button type="button" className="q-btn q-btn--argento sfide-btn" disabled={!canAct} aria-busy={busy === c.id} onClick={() => pick && void run(c.id, () => acceptChallenge(c.id, pick), true)}>
                              <span className="q-btn__in">{busy === c.id ? 'In corso…' : 'Accetta'}</span>
                            </button>
                            <button type="button" className="sfide-link" disabled={!!busy} onClick={() => void run(`${c.id}-no`, () => declineChallenge(c.id))}>Rifiuta</button>
                          </>
                        )}
                        {pending && !incoming && (
                          <button type="button" className="q-btn q-btn--acciaio sfide-btn" disabled={!!busy} onClick={() => void run(c.id, () => cancelChallenge(c.id))}>
                            <span className="q-btn__in">Ritira</span>
                          </button>
                        )}
                        {c.status === 'ACCEPTED' && <button type="button" className="sfide-link" onClick={onOpenMatches}>Vedi risultato</button>}
                      </div>
                    </div>
                  </article>
                )
              })}
            </div>
          )}
        </section>

        <aside className="sfide-lato" aria-label="Nuova sfida">
          <form className={`q-tela q-tela--arco ${pendingReceived ? 'q-braci' : ''}`} onSubmit={send}>
            <div className="q-tela__in sfide-form">
              <h2 className="q-elenco__titolo">Sfida un giocatore</h2>
              <label htmlFor="sfida-nome">Nome del giocatore</label>
              <input id="sfida-nome" className="sfide-campo" value={opponent} maxLength={32} autoComplete="off" onChange={(e) => setOpponent(e.target.value)} />
              <small>Può essere offline: ha 48 ore per rispondere.</small>
              <button type="submit" className="q-btn q-btn--argento" disabled={!canAct || !opponent.trim()} aria-busy={busy === 'nuova'}>
                <span className="q-btn__in">{busy === 'nuova' ? 'Invio…' : 'Invia sfida'}</span>
              </button>
            </div>
            <span className="q-borchia q-borchia--n" />
          </form>

          <div className="q-tela">
            <div className="q-tela__in sfide-form">
              <h2 className="q-elenco__titolo">I tuoi mazzi</h2>
              {FACTION_IDS.map((f) => (
                <div key={f} className="sfide-mazzo">
                  <label htmlFor={`sfida-mazzo-${f}`}>{factions[f].name}</label>
                  <select id={`sfida-mazzo-${f}`} className="sfide-campo" value={deckFor(f)?.id ?? ''} disabled={usable(f).length === 0} onChange={(e) => setChosen((c) => ({ ...c, [f]: e.target.value }))}>
                    {usable(f).length === 0 && <option value="">Nessun mazzo valido</option>}
                    {usable(f).map((d) => (
                      <option key={d.id} value={d.id}>{d.name}{d.defense ? ' · Difesa' : ''}</option>
                    ))}
                  </select>
                </div>
              ))}
              <span id="sfida-inizio" className="sfide-etichetta">Prima partita con</span>
              <div role="radiogroup" aria-labelledby="sfida-inizio" className="sfide-inizio" onKeyDown={onRadioKeyDown}>
                {FACTION_IDS.map((f) => (
                  <button key={f} type="button" role="radio" aria-checked={start === f} tabIndex={start === f ? 0 : -1} onClick={() => setStart(f)}>{factions[f].name}</button>
                ))}
              </div>
              {blocked && !demo && (
                <p className="m-regola m-regola--errore">
                  {blocked} <button type="button" className="play-link" onClick={() => onNavigate('mazzi')}>Gestisci mazzi</button>
                </p>
              )}
              <button
                type="button"
                className="q-btn q-btn--acciaio"
                disabled={!canAct || !!roomOpen}
                aria-busy={busy === 'stanza'}
                onClick={openMyRoom}
              >
                <span className="q-btn__in">{roomOpen ? 'Stanza già aperta' : busy === 'stanza' ? 'Apertura…' : 'Apri una stanza'}</span>
              </button>
              <small>La stanza resta aperta 10 minuti.</small>
            </div>
          </div>
        </aside>
      </div>

      {battle && <BattleScreen key={battle.id} series={battle} onClose={() => setBattle(null)} onOpenMatches={onOpenMatches} />}
    </main>
  )
}
