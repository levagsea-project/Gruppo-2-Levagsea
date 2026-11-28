import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { createDemoTrades } from '../../data/demoTrades'
import { ApiError, GENERIC } from '../../lib/api'
import { isDemoSession } from '../../lib/auth'
import { getCollection } from '../../lib/cards'
import { getDeck, listDecks } from '../../lib/decks'
import { onRadioKeyDown } from '../../lib/radioKeys'
import {
  backendTrades, cardsIGet, cardsIGive, cardsLabel, initials, isMissingApi, otherPlayer, tabOf, tradeError, when,
  type Friend, type FriendRequests, type Player, type Trade, type TradeTab, type TradesApi,
} from '../../lib/trades'
import type { OwnedCard } from '../../types/deck'
import CardFace from '../cards/CardFace'
import PageTitle from '../layout/PageTitle'
import TradeProposal, { type Draft } from './TradeProposal'
import '../../styles/collection.css'
import '../../styles/challenges.css' // campo di testo e riquadro dell'esito, come in Sfide
import '../../styles/trades.css'

// Scambi (design: Pagina-Scambi). Tre colonne: amici (aggiungi, richieste, elenco) | proposte per scheda | dettaglio
// della proposta scelta con le azioni. "Scambia" e "Nuova proposta" aprono la composizione (TradeProposal).
// Gli endpoint non esistono ancora nel backend: vedi lib/trades.ts per cosa succede finché mancano.

type Source = 'caricamento' | 'backend' | 'prova' | 'mancante' | 'errore'
const TABS: [TradeTab, string][] = [['arrivo', 'In arrivo'], ['inviate', 'Inviate'], ['concluse', 'Concluse']]
const OUTCOME: Record<Trade['status'], string> = { PENDING: '', ACCEPTED: 'Accettato', REJECTED: 'Rifiutato', CANCELLED: 'Annullato' }

export default function TradesPage() {
  const demoSession = isDemoSession()
  const [source, setSource] = useState<Source>('caricamento')
  const [api, setApi] = useState<TradesApi>(backendTrades)
  const [loadError, setLoadError] = useState('')
  const [mine, setMine] = useState<OwnedCard[]>([])
  const [defense, setDefense] = useState<Set<string>>(new Set()) // carte dei mazzi di difesa: non si scambiano
  const [friends, setFriends] = useState<Friend[]>([])
  const [requests, setRequests] = useState<FriendRequests>({ received: [], sent: [] })
  const [trades, setTrades] = useState<Trade[]>([])
  const [tab, setTab] = useState<TradeTab>('arrivo')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [draft, setDraft] = useState<Draft | null>(null) // proposta in composizione: si apre la seconda schermata
  const [busy, setBusy] = useState('')
  const [notice, setNotice] = useState<{ text: string; error?: boolean } | null>(null)

  // Amici, richieste e scambi: si rileggono dopo ogni azione.
  const refresh = useCallback(async (from: TradesApi) => {
    const [f, r, t] = await Promise.all([from.listFriends(), from.listFriendRequests(), from.listTrades()])
    setFriends(f)
    setRequests(r)
    setTrades(t)
  }, [])

  // All'apertura: collezione e mazzi di difesa (servono alle proposte), poi amici e scambi dal backend.
  // Se il backend non ha ancora gli endpoint: in sviluppo i dati di prova, altrimenti l'avviso.
  const load = useCallback(async () => {
    try {
      const [col, decks] = await Promise.all([getCollection(), listDecks()])
      setMine(col.cards)
      const details = await Promise.all(decks.filter((d) => d.defense).map((d) => getDeck(d.id)))
      setDefense(new Set(details.flatMap((d) => d.cardList.map((c) => c.instanceId))))
      try {
        await refresh(backendTrades)
        setApi(backendTrades)
        setSource('backend')
      } catch (err) {
        if (!isMissingApi(err)) throw err
        if (!import.meta.env.DEV) return setSource('mancante')
        const demo = createDemoTrades(col.cards)
        await refresh(demo)
        setApi(demo)
        setSource('prova')
      }
    } catch (err) {
      setLoadError(err instanceof ApiError ? tradeError(err) : GENERIC)
      setSource('errore')
    }
  }, [refresh])

  useEffect(() => {
    if (!demoSession) void load()
  }, [demoSession, load])
  const retry = () => {
    setSource('caricamento')
    setLoadError('')
    void load()
  }

  // Esegue un'azione, mostra l'esito e rilegge tutto. Dice se è andata a buon fine.
  const run = async (key: string, action: () => Promise<unknown>, done?: string) => {
    setBusy(key)
    setNotice(null)
    try {
      await action()
      if (done) setNotice({ text: done })
      return true
    } catch (err) {
      setNotice({ text: tradeError(err), error: true })
      return false
    } finally {
      setBusy('')
      await refresh(api).catch(() => undefined)
    }
  }

  const byTab = useMemo(() => {
    const out: Record<TradeTab, Trade[]> = { arrivo: [], inviate: [], concluse: [] }
    for (const t of trades) out[tabOf(t)].push(t)
    return out
  }, [trades])
  const list = byTab[tab]
  const selected = list.find((t) => t.id === selectedId) ?? list[0] ?? null

  if (draft) {
    return (
      <TradeProposal
        api={api}
        friends={friends}
        mine={mine}
        defense={defense}
        draft={draft}
        onBack={() => setDraft(null)}
        onSent={async (sent) => {
          // una controproposta sostituisce quella ricevuta: dopo l'invio, quella si rifiuta
          if (draft.replaces) await api.declineTrade(draft.replaces).catch(() => undefined)
          await refresh(api).catch(() => undefined)
          setTab('inviate')
          setSelectedId(sent.id)
        }}
      />
    )
  }

  const head = <PageTitle title="SCAMBI" subtitle="SOLO TRA AMICI" /> // senza la barra delle valute: qui non serve

  if (demoSession || (source !== 'backend' && source !== 'prova')) {
    return (
      <main className="page scambi-page">
        {head}
        <div className="coll-vuoto" role={source === 'errore' ? 'alert' : 'status'}>
          {demoSession ? (
            <p>In modalità demo gli scambi non sono disponibili: serve il backend.</p>
          ) : source === 'caricamento' ? (
            <p>Caricamento degli scambi…</p>
          ) : source === 'mancante' ? (
            <p>Gli scambi tra amici arrivano presto: il server non li gestisce ancora.</p>
          ) : (
            <>
              <p>{loadError}</p>
              <button type="button" className="q-btn q-btn--acciaio" onClick={retry}><span className="q-btn__in">Riprova</span></button>
            </>
          )}
        </div>
      </main>
    )
  }

  // Scambi solo tra amici (documento API, "Scambi tra amici"): ogni strada verso una proposta passa da qui.
  const isFriend = (username: string) => friends.some((f) => f.username === username)
  const propose = (friend: string) => isFriend(friend) && setDraft({ friend, give: [], get: [] })
  // accettata, rifiutata o annullata: la proposta passa tra le concluse e resta quella scelta
  const settle = async (t: Trade, action: () => Promise<unknown>, done: string) => {
    if (!(await run(`scambio-${t.id}`, action, done))) return
    setTab('concluse')
    setSelectedId(t.id)
  }

  return (
    <main className="page scambi-page">
      {head}
      {source === 'prova' && (
        <p className="scambi-prova" role="note">
          <b>Anteprima con dati di prova.</b> Il server non ha ancora gli scambi: amici e proposte sono inventati, le carte sono le tue. Niente viene salvato.
        </p>
      )}
      {notice && <p className={`sfide-esito ${notice.error ? 'sfide-esito--errore' : ''}`} role={notice.error ? 'alert' : 'status'}>{notice.text}</p>}

      <div className="scambi-corpo">
        <FriendsPanel
          friends={friends}
          requests={requests}
          busy={busy}
          onAdd={(name) => run('aggiungi', () => api.sendFriendRequest(name), `Richiesta di amicizia inviata a ${name}.`)}
          onAccept={(r) => run(`rich-${r.id}`, () => api.acceptFriendRequest(r.id), `${r.displayName} è ora tra i tuoi amici.`)}
          onDecline={(r) => run(`rich-${r.id}`, () => api.declineFriendRequest(r.id))}
          onTrade={propose}
        />

        <section className="scambi-proposte" aria-labelledby="scambi-proposte-titolo">
          <div className="q-marker">
            <span id="scambi-proposte-titolo" className="q-marker__title">Proposte</span>
            <span className="q-marker__line" />
            {friends.length > 0 && (
              <button type="button" className="scambi-nuova" onClick={() => propose(friends[0].username)}>+ Nuova proposta</button>
            )}
          </div>
          <div className="coll-chips" role="radiogroup" aria-label="Tipo di proposta" onKeyDown={onRadioKeyDown}>
            {TABS.map(([id, label]) => (
              <button key={id} type="button" role="radio" className="m-chip" aria-checked={tab === id} tabIndex={tab === id ? 0 : -1} onClick={() => { setTab(id); setSelectedId(null) }}>
                {label} <span className="m-chip__n">{byTab[id].length}</span>
              </button>
            ))}
          </div>
          <div className="scambi-righe">
            {list.map((t) => {
              const other = otherPlayer(t)
              const verb = tab === 'arrivo' ? 'Da' : tab === 'inviate' ? 'A' : 'Con'
              return (
                <button key={t.id} type="button" className="scambi-riga" aria-current={selected?.id === t.id} onClick={() => setSelectedId(t.id)}>
                  <Initials player={other} />
                  <span className="scambi-riga__testo">
                    <b>{verb} {other.displayName}</b>
                    <small>Ricevi {cardsLabel(cardsIGet(t).length)} · Dai {cardsLabel(cardsIGive(t).length)}</small>
                  </span>
                  <span className="scambi-riga__stato">
                    {tab === 'arrivo' ? <span className="scambi-nuova-tag">Nuova</span> : <span className="scambi-esito">{tab === 'inviate' ? 'In attesa' : OUTCOME[t.status]}</span>}
                    <small>{when(t.createdAt)}</small>
                  </span>
                </button>
              )
            })}
            {list.length === 0 && <p className="scambi-vuoto">Nessuna proposta qui.</p>}
          </div>
          <p className="scambi-nota">Puoi scambiare carte solo con i giocatori nella tua lista amici.</p>
        </section>

        <TradeDetail
          trade={selected}
          friendNow={!!selected && isFriend(otherPlayer(selected).username)}
          busy={busy}
          onAccept={(t) => settle(t, () => api.acceptTrade(t.id), 'Scambio accettato: le carte sono nella tua collezione.')}
          onDecline={(t) => settle(t, () => api.declineTrade(t.id), 'Proposta rifiutata.')}
          onCancel={(t) => settle(t, () => api.cancelTrade(t.id), 'Proposta annullata.')}
          onCounter={(t) =>
            isFriend(otherPlayer(t).username) &&
            setDraft({ friend: otherPlayer(t).username, give: cardsIGive(t).map((c) => c.instanceId), get: cardsIGet(t).map((c) => c.instanceId), replaces: t.id })
          }
        />
      </div>
    </main>
  )
}

// ---------- Iniziali nell'ogiva d'argento ----------
function Initials({ player }: { player: Player }) {
  return (
    <span className="scambi-ini" aria-hidden="true">
      <span className="q-ogiva"><span className="q-ogiva__in">{initials(player)}</span></span>
    </span>
  )
}

// ---------- Colonna degli amici ----------
type FriendsProps = {
  friends: Friend[]
  requests: FriendRequests
  busy: string
  onAdd: (username: string) => Promise<boolean>
  onAccept: (r: FriendRequests['received'][number]) => void
  onDecline: (r: FriendRequests['received'][number]) => void
  onTrade: (username: string) => void
}

function FriendsPanel({ friends, requests, busy, onAdd, onAccept, onDecline, onTrade }: FriendsProps) {
  const [name, setName] = useState('')
  const valid = name.trim().length >= 3
  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!valid || busy) return
    void onAdd(name.trim()).then((ok) => ok && setName(''))
  }
  const since = (iso: string) => `Amici dal ${new Date(iso).toLocaleDateString('it-IT', { day: 'numeric', month: 'short', year: 'numeric' })}`

  return (
    <section className="q-tela scambi-amici" aria-labelledby="scambi-amici-titolo">
      <div className="q-tela__in">
        <span id="scambi-amici-titolo" className="q-elenco__titolo">Amici · {friends.length}</span>
        <form className="scambi-cerca" onSubmit={submit}>
          <label className="sr-only" htmlFor="scambi-nuovo-amico">Nome del giocatore da aggiungere</label>
          <input id="scambi-nuovo-amico" className="sfide-campo scambi-campo" type="text" placeholder="Nome del giocatore" maxLength={32} autoComplete="off" value={name} onChange={(e) => setName(e.target.value)} />
          <button type="submit" className="m-chip" disabled={!valid || busy === 'aggiungi'}>Aggiungi</button>
        </form>

        {requests.received.length > 0 && (
          <div className="scambi-gruppo">
            <span className="scambi-freccia">Richieste</span>
            {requests.received.map((r) => (
              <div key={r.id} className="scambi-amico">
                <Initials player={r} />
                <span className="scambi-amico__nome"><b>{r.displayName}</b><small>{r.username}</small></span>
                <button type="button" className="m-chip" disabled={busy === `rich-${r.id}`} aria-label={`Accetta la richiesta di ${r.displayName}`} onClick={() => onAccept(r)}>Accetta</button>
                <button type="button" className="m-chip scambi-x" disabled={busy === `rich-${r.id}`} aria-label={`Rifiuta la richiesta di ${r.displayName}`} onClick={() => onDecline(r)}>✕</button>
              </div>
            ))}
          </div>
        )}

        {requests.sent.length > 0 && (
          <div className="scambi-gruppo">
            <span className="scambi-freccia">In attesa di risposta</span>
            {requests.sent.map((r) => (
              <div key={r.id} className="scambi-amico">
                <Initials player={r} />
                <span className="scambi-amico__nome"><b>{r.displayName}</b><small>Richiesta inviata {when(r.createdAt).toLowerCase()}</small></span>
              </div>
            ))}
          </div>
        )}

        <span className="scambi-freccia">I tuoi amici</span>
        {friends.length === 0 ? (
          <p className="scambi-vuoto">Non hai ancora amici. Aggiungi un giocatore con il suo nome per poter scambiare carte.</p>
        ) : (
          <ul className="scambi-elenco">
            {friends.map((f) => (
              <li key={f.username} className="scambi-amico">
                <Initials player={f} />
                <span className="scambi-amico__nome"><b>{f.displayName}</b><small>{since(f.since)}</small></span>
                <button type="button" className="m-chip" aria-label={`Proponi uno scambio a ${f.displayName}`} onClick={() => onTrade(f.username)}>Scambia</button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}

// ---------- Dettaglio della proposta scelta ----------
type DetailProps = {
  trade: Trade | null
  friendNow: boolean // l'altro è ancora tra gli amici: senza amicizia lo scambio non si può concludere
  busy: string
  onAccept: (t: Trade) => void
  onDecline: (t: Trade) => void
  onCancel: (t: Trade) => void
  onCounter: (t: Trade) => void
}

function TradeDetail({ trade: t, friendNow, busy, onAccept, onDecline, onCancel, onCounter }: DetailProps) {
  if (!t) {
    return (
      <section className="q-tela scambi-dettaglio" aria-label="Dettaglio della proposta">
        <div className="q-tela__in"><p className="scambi-vuoto">Scegli una proposta per vederne le carte.</p></div>
      </section>
    )
  }
  const other = otherPlayer(t)
  const tab = tabOf(t)
  const working = busy === `scambio-${t.id}`
  const kicker = tab === 'arrivo' ? 'Proposta ricevuta' : tab === 'inviate' ? 'Proposta inviata' : 'Scambio concluso'
  // Una proposta in attesa con chi non è più amico (amicizia tolta o giocatore bloccato) non si può concludere:
  // il backend la annulla, intanto qui si può solo rifiutarla o ritirarla.
  const noLongerFriends = t.status === 'PENDING' && !friendNow
  const outcome = noLongerFriends
    ? `Tu e ${other.displayName} non siete più amici: questo scambio non si può più concludere.`
    : tab === 'arrivo'
      ? `${other.displayName} ti propone questo scambio.`
      : tab === 'inviate'
        ? `In attesa che ${other.displayName} risponda.`
        : t.status === 'ACCEPTED'
          ? 'Scambio accettato: le carte sono nella collezione di chi le ha ricevute.'
          : t.status === 'CANCELLED'
            ? t.myRole === 'PROPOSER' ? 'Hai annullato la proposta.' : `${other.displayName} ha annullato la proposta.`
            : t.myRole === 'RECIPIENT' ? 'Hai rifiutato la proposta.' : `${other.displayName} ha rifiutato la proposta.`

  return (
    <section className="q-tela scambi-dettaglio" aria-label="Dettaglio della proposta">
      <div className="q-tela__in" aria-live="polite">
        <span className="q-elenco__titolo">{kicker}</span>
        <span className="scambi-con">
          <b>{other.displayName}</b>
          <small>{when(t.createdAt)}</small>
        </span>
        <span className="scambi-freccia">Ricevi</span>
        <Cards cards={cardsIGet(t)} />
        <span className="scambi-freccia">Dai</span>
        <Cards cards={cardsIGive(t)} />
        <p className={`m-regola ${t.status === 'ACCEPTED' ? 'm-ok' : ''} ${noLongerFriends ? 'm-regola--off' : ''}`}>{outcome}</p>

        {tab === 'arrivo' && noLongerFriends && (
          <div className="scambi-azioni">
            <button type="button" className="q-btn q-btn--acciaio" disabled={working} onClick={() => onDecline(t)}><span className="q-btn__in">Rifiuta</span></button>
          </div>
        )}
        {tab === 'arrivo' && !noLongerFriends && (
          <div className="scambi-azioni">
            <button type="button" className="q-btn q-btn--argento" disabled={working} onClick={() => onAccept(t)}><span className="q-btn__in">Accetta lo scambio</span></button>
            <div className="scambi-azioni__due">
              <button type="button" className="q-btn q-btn--acciaio" disabled={working} onClick={() => onCounter(t)}><span className="q-btn__in">Controproposta</span></button>
              <button type="button" className="q-btn q-btn--acciaio" disabled={working} onClick={() => onDecline(t)}><span className="q-btn__in">Rifiuta</span></button>
            </div>
          </div>
        )}
        {tab === 'inviate' && (
          <div className="scambi-azioni">
            <button type="button" className="q-btn q-btn--acciaio" disabled={working} onClick={() => onCancel(t)}><span className="q-btn__in">Annulla la proposta</span></button>
          </div>
        )}
      </div>
    </section>
  )
}

/** Carte piccole in griglia da tre, con il nome sotto. */
export function Cards({ cards }: { cards: OwnedCard[] }) {
  if (!cards.length) return <p className="scambi-vuoto">Nessuna carta.</p>
  return (
    <ul className="scambi-carte">
      {cards.map((c) => (
        <li key={c.instanceId} className="scambi-carta">
          <CardFace card={c} sizes="120px" />
          <small lang={c.textLocale}>{c.name}</small>
        </li>
      ))}
    </ul>
  )
}
