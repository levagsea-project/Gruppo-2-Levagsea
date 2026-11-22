import { useCallback, useEffect, useRef, useState, type CSSProperties } from 'react'
import { factions } from '../../data/factions'
import type { Page } from '../../data/navItems'
import { playModes, type PlayModeId } from '../../data/play'
import { resultLabel } from '../../data/profile'
import { ApiError, GENERIC } from '../../lib/api'
import { isDemoSession } from '../../lib/auth'
import { toFaction } from '../../lib/cards'
import { cancelRoom, createChallenge, getMyRoom, joinRoom, listRooms, openRoom, timeLeft, type Room } from '../../lib/challenges'
import { rememberSeries } from '../../lib/matches'
import { readPlayPrefs, writePlayPrefs } from '../../lib/playPrefs'
import { onRadioKeyDown } from '../../lib/radioKeys'
import { getSeries, markSeriesSeen, playMatchmaking, seriesScore, type DeckPick, type SeriesResponse } from '../../lib/series'
import type { Deck, FactionId, PlayStatus } from '../../types/deck'
import type { LoadState } from '../../hooks/useDecks'
import { playBlockReason } from '../../hooks/usePlayStatus'
import BattleScreen from '../battle/BattleScreen'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import DeckSignature from './DeckSignature'
import '../../styles/challenges.css'
import '../../styles/play.css'

const Star = () => (
  <svg className="q-stella" aria-hidden="true" viewBox="0 0 20 28">
    <path d="M10 0l2.2 11.6L20 14l-7.8 2.4L10 28l-2.2-11.6L0 14l7.8-2.4z" fill="#8FD6DB" />
  </svg>
)

const FACTION_IDS: FactionId[] = ['artificiali', 'umani']

// Errori dopo i quali la lista dei mazzi o "posso giocare?" non sono più aggiornati.
const STALE_DECKS = new Set(['deck.outdated', 'deck.defense_required', 'deck.not_found'])

const POLL_MS = 10_000 // il backend non manda notifiche: le stanze si rileggono a intervalli

// Messaggio dopo un'azione, con l'eventuale collegamento a dove continuare.
type Outcome = { text: string; link?: 'profilo' | 'sfide'; again?: boolean }

// Errori passeggeri: ha senso riprovare la stessa azione.
const RETRY_CODES = new Set(['engine.unavailable', 'matchmaking.no_opponent'])
const canRetryError = (err: unknown) => err instanceof ApiError && (err.status === 0 || (!!err.code && RETRY_CODES.has(err.code)))

// Il messaggio che resta sulla pagina dopo la battaglia.
const playedOutcome = (series: SeriesResponse): Outcome => {
  const s = seriesScore(series)
  return { text: `${resultLabel[s.result]} ${s.won}–${s.lost} contro ${s.opponent}.`, link: 'profilo', again: true }
}

type RoomTab = 'entra' | 'apri'

type Props = {
  decks: Deck[]
  decksState: LoadState
  playStatus: PlayStatus | null
  playStatusFailed: boolean
  onNavigate: (page: Page) => void
  onOpenMatches: () => void
  onDecksChanged: () => void // rilegge mazzi e "posso giocare?"
}

// Pagina Gioca: 1) modalità in alto, 2) i mazzi al centro, 3) a destra il leggìo per avviare la modalità scelta.
// Una serie è al meglio delle 3 e le fazioni si scambiano a ogni partita, per questo serve un mazzo per fazione.
// Si gioca solo con mazzi non obsoleti e con un mazzo di difesa valido per entrambe le fazioni (GET /decks/play-status).
// Le modalità sono i tre avvii del backend: POST /matchmaking, /rooms (apri o entra) e /challenges.
// La serie finisce nello storico del backend (profilo); il browser ricorda turni e nomi dei mazzi (lib/matches.ts).
// All'apertura ripropone l'ultima configurazione (lib/playPrefs.ts), così chi ha i mazzi pronti parte con un clic.
export default function PlayPage({ decks, decksState, playStatus, playStatusFailed, onNavigate, onOpenMatches, onDecksChanged }: Props) {
  const [prefs] = useState(readPlayPrefs)
  const [modeId, setModeId] = useState<PlayModeId>(prefs.mode)
  const [chosen, setChosen] = useState<Partial<Record<FactionId, string>>>(prefs.decks)
  const [start, setStart] = useState<FactionId>(prefs.start)
  const [busy, setBusy] = useState('') // azione in corso: 'avvio' o il codice della stanza
  const [error, setError] = useState('')
  const [canRetry, setCanRetry] = useState(false)
  const [outcome, setOutcome] = useState<Outcome | null>(null)
  const [rooms, setRooms] = useState<Room[] | null>(null)
  const [myRoom, setMyRoom] = useState<Room | null>(null)
  const [roomsError, setRoomsError] = useState('')
  const [opponent, setOpponent] = useState('')
  const [roomTab, setRoomTab] = useState<RoomTab | null>(null) // scheda scelta in Stanza; null = la decide la situazione
  const [battle, setBattle] = useState<SeriesResponse | null>(null) // la serie da animare a tutto schermo
  const loadSeq = useRef(0) // scarta le risposte arrivate dopo una lettura più recente
  const waitingCode = useRef<string | null>(null) // la mia stanza che era aperta all'ultima lettura
  const roomDeckNames = useRef<Record<FactionId, string> | null>(null) // i mazzi con cui ho aperto la stanza
  const firstRoomsLoad = useRef(true)
  const corpo = useRef<HTMLDivElement>(null)

  const loadRooms = useCallback(async () => {
    const seq = ++loadSeq.current
    try {
      const [open, mine] = await Promise.all([listRooms(), getMyRoom()])
      if (seq !== loadSeq.current) return
      setRooms(open)
      setMyRoom(mine)
      setRoomsError('')

      // Tornando sulla pagina con una stanza ancora aperta si riparte da lì.
      if (firstRoomsLoad.current) {
        firstRoomsLoad.current = false
        if (mine?.status === 'OPEN') setModeId('stanza')
        // La stanza è stata giocata mentre non ero su questa pagina: se l'esito non è ancora stato letto, lo mostro qui.
        if (mine?.status === 'MATCHED' && mine.seriesId) {
          void getSeries(mine.seriesId)
            .then((series) => {
              if (!series.unread) return
              const played = playedOutcome(series)
              setOutcome((now) => now ?? { text: `Mentre eri via qualcuno è entrato nella tua stanza. ${played.text}`, link: 'profilo' })
              void markSeriesSeen(series.id).catch(() => {}) // se fallisce riapparirà tra i non letti
            })
            .catch(() => {}) // è una novità in più: se non arriva, la serie resta comunque nel profilo
        }
      }

      // La mia stanza era aperta e ora non lo è più: chi aspetta scopre così com'è andata.
      const waited = waitingCode.current
      waitingCode.current = mine?.status === 'OPEN' ? mine.code : null
      if (!mine || mine.code !== waited) return
      if (mine.status === 'EXPIRED') setOutcome({ text: 'La tua stanza è scaduta senza avversari.' })
      if (mine.status === 'MATCHED') {
        setError('')
        try {
          if (!mine.seriesId) throw new Error('serie assente')
          const series = await getSeries(mine.seriesId)
          if (roomDeckNames.current) rememberSeries(series, roomDeckNames.current)
          setOutcome(playedOutcome(series))
          setBattle(series) // senza log: la battaglia li chiede al replay
          void markSeriesSeen(series.id).catch(() => {}) // il risultato è a schermo; se fallisce riapparirà tra i non letti
        } catch {
          setOutcome({ text: 'Qualcuno è entrato nella tua stanza e la serie è stata giocata.', link: 'profilo' })
        }
      }
    } catch (err) {
      if (seq === loadSeq.current) setRoomsError(err instanceof ApiError ? err.message : GENERIC)
    }
  }, [])

  useEffect(() => {
    if (isDemoSession()) return
    void loadRooms()
    const timer = window.setInterval(() => {
      if (!document.hidden) void loadRooms()
    }, POLL_MS)
    return () => window.clearInterval(timer)
  }, [loadRooms])

  // Un mazzo ricordato può stare in fondo alla fila: la fila scorre fino a mostrarlo.
  useEffect(() => {
    corpo.current?.querySelectorAll('.play-mazzi').forEach((row) => {
      const on = row.querySelector('[aria-checked="true"]')
      if (!on) return
      const r = row.getBoundingClientRect()
      const b = on.getBoundingClientRect()
      if (b.left < r.left || b.right > r.right) row.scrollLeft += b.left - r.left - 10
    })
  }, [decks.length])

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

  const roomOpen = myRoom?.status === 'OPEN' ? myRoom : null
  // Stanza ha due strade: entrare in quella di un altro o aprire la propria. Senza una scelta si parte da quella utile ora.
  const roomView: RoomTab = roomTab ?? (roomOpen || !rooms?.length ? 'apri' : 'entra')
  const joining = modeId === 'stanza' && roomView === 'entra'
  // Il backend blocca i mazzi all'apertura: finché la stanza è aperta, qui non si possono cambiare.
  const waiting = modeId === 'stanza' && roomView === 'apri' && !!roomOpen
  const name = opponent.trim()

  const decksLoading = decksState === 'loading' && decks.length === 0
  const decksFailed = decksState === 'error' && decks.length === 0

  const a = picked.artificiali
  const u = picked.umani
  const pick: DeckPick | null = a && u ? { artificiali: a.id, umani: u.id } : null
  const deckNames = a && u ? { artificiali: a.name, umani: u.name } : null

  // Se non si può giocare: il motivo e l'azione che lo risolve, al posto del pulsante di avvio.
  const toDecks = () => onNavigate('mazzi')
  const missingHasDecks = missing.some((f) => decks.some((d) => d.faction === f))
  const fix: { text: string; label: string; action: () => void } | null =
    demo || decksLoading
      ? null
      : decksFailed
        ? { text: 'Non riesco a leggere i tuoi mazzi.', label: 'Riprova', action: onDecksChanged }
        : missing.length
          ? {
              text:
                missing.length === 2
                  ? decks.length
                    ? 'I tuoi mazzi sono obsoleti: modificane uno per fazione per giocare.'
                    : 'Crea un mazzo per ogni fazione per giocare.'
                  : `Ti serve un mazzo valido ${missing[0] === 'umani' ? 'della' : 'delle'} ${factions[missing[0]].name}.`,
              label: missingHasDecks ? 'Sistema i mazzi' : 'Crea un mazzo',
              action: missingHasDecks ? toDecks : () => onNavigate('nuovo-mazzo'),
            }
          : blocked
            ? { text: blocked, label: playStatus?.missingDefense.length ? 'Scegli il mazzo di difesa' : 'Aggiorna il mazzo di difesa', action: toDecks }
            : playStatusFailed
              ? { text: 'Non riesco a controllare i tuoi mazzi di difesa.', label: 'Riprova', action: onDecksChanged }
              : null

  const otherFaction: FactionId = start === 'umani' ? 'artificiali' : 'umani'
  const statusLine = demo
    ? 'In modalità demo non si gioca: serve il backend.'
    : decksLoading
      ? 'Caricamento dei mazzi…'
      : fix
        ? fix.text
        : waiting
          ? 'Puoi chiudere la stanza finché nessuno entra.'
          : joining
            ? `Entrando giochi subito con ${picked.artificiali?.name} e ${picked.umani?.name}.`
            : modeId === 'sfida' && !name
              ? 'Scrivi il nome del giocatore che vuoi sfidare.'
              : `${picked[start]?.name} e ${picked[otherFaction]?.name}, inizi con ${factions[start].name}.`

  const canAct = !demo && !decksLoading && !fix && !busy
  const canStart = canAct && (modeId === 'libera' || (modeId === 'stanza' && roomView === 'apri' && !roomOpen) || (modeId === 'sfida' && !!name))

  // Dopo un avvio riuscito: alla prossima visita la pagina riparte da qui.
  const remember = (m: PlayModeId) => {
    if (pick) writePlayPrefs({ mode: m, start, decks: pick })
  }

  // Esegue un'azione, mostra esito o errore e rilegge le stanze.
  const run = async (id: string, action: () => Promise<Outcome>) => {
    setBusy(id)
    setError('')
    setCanRetry(false)
    setOutcome(null)
    try {
      setOutcome(await action())
    } catch (err) {
      setError(err instanceof ApiError ? err.message : GENERIC)
      setCanRetry(id === 'avvio' && canRetryError(err))
      if (err instanceof ApiError && err.code && STALE_DECKS.has(err.code)) onDecksChanged()
    } finally {
      setBusy('')
      void loadRooms()
    }
  }

  // La serie è già nello storico del backend: il browser ricorda solo turni e nomi dei mazzi.
  const finish = (series: SeriesResponse) => {
    if (deckNames) rememberSeries(series, deckNames)
    setBattle(series)
    return playedOutcome(series)
  }

  const startMode = () => {
    if (!canStart || !pick) return
    if (modeId === 'libera')
      void run('avvio', async () => {
        const series = await playMatchmaking(start, pick)
        remember('libera')
        return finish(series)
      })
    if (modeId === 'stanza')
      void run('avvio', async () => {
        const room = await openRoom(start, pick)
        remember('stanza')
        roomDeckNames.current = deckNames
        waitingCode.current = room.code
        setMyRoom(room)
        setRoomTab('apri')
        return { text: 'Stanza aperta: resta qui, il risultato arriva appena entra qualcuno.' }
      })
    if (modeId === 'sfida')
      void run('avvio', async () => {
        await createChallenge(name, start, pick)
        remember('sfida')
        setOpponent('')
        return { text: `Sfida inviata a ${name}: ha 48 ore per rispondere.`, link: 'sfide' }
      })
  }

  const join = (room: Room) => {
    if (!canAct || !pick) return
    void run(room.code, async () => {
      const series = await joinRoom(room.code, pick)
      remember('stanza')
      return finish(series)
    })
  }

  const closeRoom = (room: Room) =>
    void run(room.code, async () => {
      await cancelRoom(room.code)
      waitingCode.current = null
      return { text: 'Stanza chiusa.' }
    })

  const busyLabel = modeId === 'libera' ? 'In corso…' : modeId === 'stanza' ? 'Apertura…' : 'Invio…'
  const actionLabel =
    busy === 'avvio' ? busyLabel : modeId === 'libera' && outcome?.again ? 'Gioca ancora' : mode.action

  return (
    <main className="page play-page">
      {/* Intestazione: titolo a sinistra, Gestisci mazzi e valute a destra */}
      <header className="play-intestazione">
        <PageTitle title="GIOCA" subtitle="SCEGLI MODALITÀ E MAZZI" />
        <div className="play-intestazione__azioni">
          <button type="button" className="q-btn q-btn--acciaio play-gestisci" onClick={() => onNavigate('mazzi')}>
            <span className="q-btn__in">Gestisci mazzi</span>
          </button>
          <CurrencyBar />
        </div>
      </header>

      <div className="play-testa">
        <span className="q-divisorio play-testa__filo" aria-hidden="true">
          <span className="q-borchia q-borchia--n" />
        </span>
        <section aria-label="Scegli la modalità">
          <div role="radiogroup" aria-label="Modalità di gioco" className="play-modi" onKeyDown={onRadioKeyDown}>
            {playModes.map((m) => (
              <button key={m.id} type="button" role="radio" aria-checked={m.id === modeId} tabIndex={m.id === mode.id ? 0 : -1} className={`q-tela play-stendardo ${m.id === modeId ? 'q-acceso' : ''}`} onClick={() => { setModeId(m.id); setError('') }}>
                <span className="q-tela__in">
                  <span className="play-stendardo__orlo" aria-hidden="true" />
                  <span className="play-stendardo__nome">{m.title}</span>
                  <span className="play-stendardo__stemma" aria-hidden="true">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
                      {m.paths.map((d) => <path key={d} d={d} />)}
                    </svg>
                  </span>
                </span>
                <span className="q-borchia q-borchia--s" />
              </button>
            ))}
          </div>
        </section>
      </div>

      <div className="play-corpo" ref={corpo}>
      <section className="play-step play-step--mazzi" aria-labelledby="g-deck">
        <h2 id="g-deck" className="sr-only">Scegli i mazzi</h2>

        {decksLoading ? (
          <p className="play-nota" role="status">Caricamento dei mazzi…</p>
        ) : decksFailed ? (
          <p className="play-nota" role="alert">I mazzi non sono disponibili in questo momento.</p>
        ) : decks.length === 0 ? (
          <div className="q-tela play-vuoto">
            <div className="q-tela__in">
              <p>Non hai ancora nessun mazzo.</p>
              <button type="button" className="q-btn q-btn--argento" onClick={() => onNavigate('nuovo-mazzo')}>
                <span className="q-btn__in">Crea mazzo</span>
              </button>
            </div>
          </div>
        ) : (
          <div className={`play-fazioni ${waiting ? 'play-fazioni--ferme' : ''}`}>
            {FACTION_IDS.map((f) => {
              const faction = factions[f]
              const own = decks.filter((d) => d.faction === f)
              const groupId = `g-deck-${f}`
              // una sola tappa del Tab per fila: il mazzo scelto, oppure il primo se nessuno è sceglibile
              const tabStop = !waiting && picked[f] ? picked[f].id : own[0]?.id
              return (
                <div key={f} className="play-fazione">
                  <div className="q-marker play-fazione__testa">
                    <span className="play-rombo" aria-hidden="true" />
                    <h3 id={groupId} className="play-fazione__nome">{faction.name}</h3>
                    <span className="q-marker__line" />
                  </div>
                  {own.length === 0 ? (
                    <div className="play-fazione__vuoto">
                      <p>Nessun mazzo per questa fazione.</p>
                      <button type="button" className="q-btn q-btn--acciaio" onClick={() => onNavigate('nuovo-mazzo')}>
                        <span className="q-btn__in">Crea mazzo</span>
                      </button>
                    </div>
                  ) : (
                    <div role="radiogroup" aria-labelledby={groupId} className="play-mazzi" onKeyDown={onRadioKeyDown}>
                      {own.map((d) => {
                        const ok = usable(d) && !waiting
                        // a stanza aperta i mazzi in gioco sono quelli dell'apertura: nessuna carta risulta scelta qui
                        const on = !waiting && d.id === picked[f]?.id
                        return (
                          <button
                            key={d.id}
                            type="button"
                            role="radio"
                            aria-checked={on}
                            aria-disabled={!ok}
                            tabIndex={d.id === tabStop ? 0 : -1}
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
                                    {/* la fazione è già nel titolo della colonna: a schermo il nome non la ripete */}
                                    <span className="q-carta__nome" title={d.name}>
                                      <span aria-hidden="true">{d.name.replace(` (${faction.name})`, '')}</span>
                                      <span className="sr-only">{d.name}</span>
                                    </span>
                                    <DeckSignature deckId={d.id} />
                                    <span className={d.outdated ? 'play-conta play-conta--off' : 'play-conta'}>
                                      {d.outdated ? 'Obsoleto' : d.defense ? 'Difesa' : ''}
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

        {/* Perché servono due mazzi e che cos'è la Difesa: vale per tutte le modalità */}
        <div className="play-serie">
          <p><b>Serie al meglio delle 3.</b> Scegli un mazzo per fazione: a ogni partita le fazioni si scambiano.</p>
          <p><b>Difesa.</b> È il mazzo che ti rappresenta in Partita libera, anche quando non ci sei.</p>
          <button type="button" className="play-link" onClick={() => onNavigate('regole')}>Leggi le regole</button>
        </div>
      </section>

      {/* Il leggìo: tutto ciò che serve per avviare la modalità scelta */}
      <div className="play-lato">
        <section className="play-step play-step--lato" aria-labelledby="g-avvio">
          <div className="q-tela q-tela--arco play-leggio">
            <div className="q-tela__in">
              <h2 id="g-avvio" className="play-leggio__titolo">{mode.title}</h2>
              <span className="q-divisorio" aria-hidden="true" />
              {modeId === 'stanza' && !demo && (
                <div className="coll-chips play-strade" role="radiogroup" aria-label="Cosa vuoi fare" onKeyDown={onRadioKeyDown}>
                  <button type="button" role="radio" className="m-chip" aria-checked={roomView === 'entra'} tabIndex={roomView === 'entra' ? 0 : -1} onClick={() => setRoomTab('entra')}>
                    Entra{rooms ? <span className="m-chip__n">{rooms.length}</span> : null}
                  </button>
                  <button type="button" role="radio" className="m-chip" aria-checked={roomView === 'apri'} tabIndex={roomView === 'apri' ? 0 : -1} onClick={() => setRoomTab('apri')}>
                    {roomOpen ? 'La tua stanza' : 'Apri la tua'}
                  </button>
                </div>
              )}
              <p className="play-leggio__desc">
                {modeId !== 'stanza' || demo
                  ? mode.desc
                  : joining
                    ? 'Scegli chi sta aspettando: la serie parte subito con i mazzi selezionati.'
                    : roomOpen
                      ? 'In attesa di un avversario. La stanza usa i mazzi con cui l’hai aperta.'
                      : 'Apri una stanza con i mazzi selezionati e aspetta che entri qualcuno.'}
              </p>
              <p className="play-leggio__meta">{mode.meta}</p>

              {modeId === 'stanza' &&
                (demo ? (
                  <p className="play-nota">In modalità demo le stanze non sono disponibili.</p>
                ) : roomsError && !rooms ? (
                  <p className="play-nota" role="alert">
                    {roomsError}{' '}
                    <button type="button" className="play-link" onClick={() => void loadRooms()}>Riprova</button>
                  </p>
                ) : !rooms ? (
                  <p className="play-nota" role="status">Caricamento delle stanze…</p>
                ) : joining || waiting ? (
                  <div className="play-stanze">
                    {waiting && roomOpen && (
                      <article className="play-stanza play-stanza--mia">
                        <div>
                          <h3>{roomOpen.code}</h3>
                          <p>Inizi con {factions[toFaction(roomOpen.creatorFaction)].name} · scade tra {timeLeft(roomOpen.expiresAt)}</p>
                        </div>
                        <button type="button" className="q-btn q-btn--acciaio sfide-btn" disabled={!!busy} aria-busy={busy === roomOpen.code} onClick={() => closeRoom(roomOpen)}>
                          <span className="q-btn__in">Chiudi</span>
                        </button>
                      </article>
                    )}
                    {joining && rooms.map((r) => {
                      // Chi entra gioca la prima partita con la fazione opposta a quella del creatore.
                      const mine: FactionId = toFaction(r.creatorFaction) === 'umani' ? 'artificiali' : 'umani'
                      return (
                        <article key={r.id} className="play-stanza">
                          <div>
                            <h3>{r.creator.username}</h3>
                            <p>Inizi con {factions[mine].name} · scade tra {timeLeft(r.expiresAt)}</p>
                          </div>
                          <button type="button" className="q-btn q-btn--argento sfide-btn" disabled={!canAct} aria-busy={busy === r.code} onClick={() => join(r)}>
                            <span className="q-btn__in">{busy === r.code ? 'In corso…' : 'Entra'}</span>
                          </button>
                        </article>
                      )
                    })}
                    {joining && rooms.length === 0 && (
                      <p className="play-nota">
                        Nessuno sta aspettando in questo momento.{' '}
                        <button type="button" className="play-link" onClick={() => setRoomTab('apri')}>{roomOpen ? 'Torna alla tua stanza' : 'Apri la tua'}</button>
                      </p>
                    )}
                  </div>
                ) : null)}

              {modeId === 'sfida' && (
                <div className="play-sfida">
                  <label htmlFor="g-avversario">Nome del giocatore</label>
                  <input
                    id="g-avversario"
                    className="sfide-campo"
                    value={opponent}
                    maxLength={32}
                    autoComplete="off"
                    onChange={(e) => setOpponent(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') startMode()
                    }}
                  />
                </div>
              )}

              {/* Fazione della prima partita: nella seconda le fazioni si scambiano */}
              {!joining && !waiting && (
              <div className="play-inizio">
                <span id="g-start" className="play-inizio__etichetta">Prima partita con</span>
                {/* Bilancia: i due nomi ai lati di un filo, il rombo scivola verso la fazione scelta */}
                <div role="radiogroup" aria-labelledby="g-start" className="play-bilancia" data-lato={start === FACTION_IDS[0] ? 'sinistra' : 'destra'} onKeyDown={onRadioKeyDown}>
                  {FACTION_IDS.map((f) => (
                    <button key={f} type="button" role="radio" aria-checked={start === f} tabIndex={start === f ? 0 : -1} className="play-bilancia__lato" onClick={() => setStart(f)}>
                      {factions[f].name}
                    </button>
                  ))}
                  <span className="play-bilancia__asta" aria-hidden="true">
                    <span className="play-bilancia__rombo" />
                  </span>
                </div>
              </div>
              )}
            </div>
            <span className="q-borchia q-borchia--n" />
          </div>
        </section>

        <div className="play-azione">
          {/* un solo annuncio: la zona è viva, i messaggi dentro non hanno un ruolo proprio */}
          <div className="play-riepilogo" role="status" aria-atomic="true">
            {busy && busy !== 'avvio' ? (
              <small>Operazione in corso…</small>
            ) : busy && modeId === 'libera' ? (
              <small>Partita in corso: il motore sta giocando la serie…</small>
            ) : error ? (
              <small className="play-errore">
                {error}{' '}
                {canRetry && !fix && (
                  <button type="button" className="play-link" onClick={startMode}>Riprova</button>
                )}
              </small>
            ) : outcome ? (
              <small>
                {outcome.text}{' '}
                {outcome.link === 'profilo' && (
                  <button type="button" className="play-link" onClick={onOpenMatches}>Vedi nel profilo</button>
                )}
                {outcome.link === 'sfide' && (
                  <button type="button" className="play-link" onClick={() => onNavigate('sfide')}>Vai a Sfide</button>
                )}
              </small>
            ) : (
              <small>{statusLine}</small>
            )}
          </div>
          {fix ? (
            <button type="button" className="q-btn q-btn--argento play-rimedio" onClick={fix.action}>
              <span className="q-btn__in">{fix.label}</span>
            </button>
          ) : joining || waiting ? null : (
            <button type="button" className="q-btn q-btn--sigillo" disabled={!canStart} aria-busy={busy === 'avvio'} onClick={startMode}>
              <span className="q-btn__in">
                <Star />
                {actionLabel}
              </span>
              <span className="q-borchia q-borchia--w" />
              <span className="q-borchia q-borchia--e" />
            </button>
          )}
        </div>
      </div>
      </div>

      {battle && <BattleScreen key={battle.id} series={battle} onClose={() => setBattle(null)} onOpenMatches={onOpenMatches} />}
    </main>
  )
}
