import { useEffect, useMemo, useState } from 'react'
import { onRadioKeyDown } from '../../lib/radioKeys'
import { TRADE_MAX_PER_SIDE, tradeError, type Friend, type Trade, type TradesApi } from '../../lib/trades'
import type { OwnedCard } from '../../types/deck'
import CardFace from '../cards/CardFace'
import PageTitle from '../layout/PageTitle'

/** Proposta da comporre: amico e carte già scelte (instanceId). replaces = la proposta a cui si risponde. */
export type Draft = { friend: string; give: string[]; get: string[]; replaces?: string }

type Props = {
  api: TradesApi
  friends: Friend[]
  mine: OwnedCard[]
  defense: Set<string> // carte nei mazzi di difesa: non si possono dare
  draft: Draft
  onBack: () => void
  onSent: (trade: Trade) => Promise<void>
}

type Entry = { card: OwnedCard; ids: string[]; free: string[] } // tutte le copie e quelle che si possono scambiare
type Filter = 'doppioni' | 'tutte'

function group(cards: OwnedCard[], blocked: Set<string>): Entry[] {
  const byId = new Map<string, Entry>()
  for (const c of cards) {
    const e = byId.get(c.cardId) ?? { card: c, ids: [], free: [] }
    e.ids.push(c.instanceId)
    if (!blocked.has(c.instanceId)) e.free.push(c.instanceId)
    byId.set(c.cardId, e)
  }
  return [...byId.values()].sort((a, b) => a.card.name.localeCompare(b.card.name, 'it'))
}

// Proponi scambio (design: Pagina-Scambi-Proposta). Si sceglie l'amico, poi le proprie carte da dare e le sue da
// ricevere: una copia per carta. A destra il riepilogo e l'invio. Le carte del mazzo di difesa restano fuori.
export default function TradeProposal({ api, friends, mine, defense, draft, onBack, onSent }: Props) {
  const [friend, setFriend] = useState(draft.friend)
  const [give, setGive] = useState<string[]>(draft.give)
  const [get, setGet] = useState<string[]>(draft.get)
  const [filter, setFilter] = useState<Filter>(draft.give.length ? 'tutte' : 'doppioni')
  // carte dell'amico come sono arrivate, con l'amico a cui appartengono: cambiando amico tornano "in caricamento"
  const [loaded, setLoaded] = useState<{ friend: string; cards: OwnedCard[] | null; error: string } | null>(null)
  const theirs = loaded?.friend === friend ? loaded.cards : null
  const loadError = loaded?.friend === friend ? loaded.error : ''
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [sent, setSent] = useState(false)
  // Solo tra amici: con un giocatore che non è (più) tra gli amici non si caricano carte e non si invia nulla.
  const other = friends.find((f) => f.username === friend)
  const isFriend = !!other
  const name = other?.displayName ?? friend

  // Le carte scambiabili dell'amico scelto. Cambiando amico si azzerano quelle da ricevere (non quelle della proposta
  // di partenza, che sono di quell'amico).
  useEffect(() => {
    if (!isFriend) return
    let current = true
    api
      .getFriendCollection(friend)
      .then((cards) => current && setLoaded({ friend, cards, error: '' }))
      .catch((err) => current && setLoaded({ friend, cards: null, error: tradeError(err) }))
    return () => {
      current = false
    }
  }, [api, friend, isFriend])

  const pickFriend = (username: string) => {
    if (username === friend) return
    setFriend(username)
    setGet([])
    setSent(false)
    setError('')
  }

  const myEntries = useMemo(() => group(mine, defense), [mine, defense])
  const shown = filter === 'tutte' ? myEntries : myEntries.filter((e) => e.ids.length > 1)
  const theirEntries = useMemo(() => group(theirs ?? [], new Set()), [theirs])
  const owned = useMemo(() => new Set(mine.map((c) => c.cardId)), [mine])

  // Una copia per carta: premere la sceglie (la prima libera), premere di nuovo la toglie.
  const toggle = (list: string[], set: (v: string[]) => void, e: Entry) => {
    setSent(false)
    setError('')
    const chosen = e.ids.find((id) => list.includes(id))
    if (chosen) return set(list.filter((id) => id !== chosen))
    if (list.length >= TRADE_MAX_PER_SIDE) return setError(`Al massimo ${TRADE_MAX_PER_SIDE} carte per parte.`)
    if (e.free[0]) set([...list, e.free[0]])
  }

  const nameOf = (id: string, from: OwnedCard[]) => from.find((c) => c.instanceId === id)?.name ?? 'Carta'
  const ready = give.length > 0 && get.length > 0 && !!other
  const send = async () => {
    if (!ready || sending || sent) return
    setSending(true)
    setError('')
    try {
      const trade = await api.createTrade(friend, give, get)
      setSent(true)
      await onSent(trade)
    } catch (err) {
      setError(tradeError(err))
    } finally {
      setSending(false)
    }
  }

  const cell = (e: Entry, list: string[], set: (v: string[]) => void, mineSide: boolean) => {
    const on = e.ids.some((id) => list.includes(id))
    const locked = mineSide && e.free.length === 0 // tutte le copie nel mazzo di difesa
    const missing = !mineSide && !owned.has(e.card.cardId)
    const tag = locked ? 'Difesa' : missing ? 'Ti manca' : ''
    const label = [e.card.name, `${e.ids.length} ${e.ids.length === 1 ? 'copia' : 'copie'}`, missing && 'ti manca', locked && 'nel mazzo di difesa, non scambiabile', on && 'scelta'].filter(Boolean).join(', ')
    return (
      <li key={e.card.cardId}>
        <button type="button" className={`g-cella scambi-cella ${on ? 'g-cella--on scambi-cella--on' : ''}`} aria-pressed={on} aria-label={label} disabled={locked} onClick={() => toggle(list, set, e)}>
          <span className="g-faccia">
            <CardFace card={e.card} sizes="(max-width: 767px) 30vw, 130px" />
            {e.ids.length > 1 && <span className="g-copie">×{e.ids.length}</span>}
            {tag && <span className={`scambi-tag ${locked ? 'scambi-tag--difesa' : ''}`}>{tag}</span>}
            <span className="scambi-spunta" aria-hidden="true">
              <svg viewBox="0 0 12 12"><path d="M2.5 6.2 5 8.6l4.5-5" /></svg>
            </span>
          </span>
          <span className="g-nome scambi-nome" aria-hidden="true" lang={e.card.textLocale}>{e.card.name}</span>
        </button>
      </li>
    )
  }

  const status = !isFriend
    ? `${name} non è tra i tuoi amici: puoi proporre scambi solo agli amici.`
    : sent
      ? `Proposta inviata a ${name}. La trovi tra le proposte inviate.`
      : `Lo scambio avviene quando ${name} accetta. Puoi annullarlo finché è in attesa.`

  return (
    <main className="page scambi-page scambi-page--proposta">
      <PageTitle title={draft.replaces ? 'CONTROPROPOSTA' : 'PROPONI SCAMBIO'} subtitle={`CON ${name.toUpperCase()}`} />

      <div className="scambi-barra">
        <button type="button" className="m-chip" onClick={onBack}>‹ Scambi</button>
        <span className="q-elenco__titolo">Amico</span>
        <div className="coll-chips" role="radiogroup" aria-label="Scegli l'amico" onKeyDown={onRadioKeyDown}>
          {friends.map((f) => (
            <button key={f.username} type="button" role="radio" className="m-chip" aria-checked={f.username === friend} tabIndex={f.username === friend ? 0 : -1} onClick={() => pickFriend(f.username)}>
              {f.displayName}
            </button>
          ))}
        </div>
      </div>

      <div className="scambi-composizione">
        <section className="scambi-lato" aria-labelledby="scambi-mie">
          <div className="q-marker">
            <span id="scambi-mie" className="q-marker__title">Le tue carte</span>
            <span className="q-marker__line" />
            <span className="scambi-conta">Dai {give.length}</span>
          </div>
          <div className="coll-chips" role="radiogroup" aria-label="Filtra le tue carte" onKeyDown={onRadioKeyDown}>
            {(['doppioni', 'tutte'] as Filter[]).map((f) => (
              <button key={f} type="button" role="radio" className="m-chip" aria-checked={filter === f} tabIndex={filter === f ? 0 : -1} onClick={() => setFilter(f)}>
                {f === 'doppioni' ? 'Doppioni' : 'Tutte'}
              </button>
            ))}
          </div>
          <div className="scambi-griglia-box">
            {shown.length ? (
              <ul className="scambi-griglia">{shown.map((e) => cell(e, give, setGive, true))}</ul>
            ) : (
              <p className="scambi-vuoto">{filter === 'doppioni' ? 'Non hai doppioni. Scegli «Tutte» per vedere ogni carta.' : 'Non hai carte da scambiare.'}</p>
            )}
          </div>
        </section>

        <section className="scambi-lato" aria-labelledby="scambi-sue">
          <div className="q-marker">
            <span id="scambi-sue" className="q-marker__title">Carte di {name}</span>
            <span className="q-marker__line" />
            <span className="scambi-conta">Ricevi {get.length}</span>
          </div>
          <p className="scambi-nota scambi-nota--alta">Le carte segnate «Ti manca» non sono ancora nella tua collezione.</p>
          <div className="scambi-griglia-box">
            {!isFriend ? (
              <p className="scambi-vuoto">Puoi vedere e chiedere le carte solo dei tuoi amici.</p>
            ) : loadError ? (
              <p className="scambi-vuoto" role="alert">{loadError}</p>
            ) : !theirs ? (
              <p className="scambi-vuoto" role="status">Caricamento delle carte di {name}…</p>
            ) : theirEntries.length ? (
              <ul className="scambi-griglia">{theirEntries.map((e) => cell(e, get, setGet, false))}</ul>
            ) : (
              <p className="scambi-vuoto">{name} non ha carte da scambiare.</p>
            )}
          </div>
        </section>

        <section className="q-tela scambi-riepilogo" aria-label="Riepilogo della proposta">
          <div className="q-tela__in" aria-live="polite">
            <span className="q-elenco__titolo">Riepilogo</span>
            <span className="scambi-freccia">Dai · {give.length}</span>
            <ul className="scambi-nomi">
              {give.map((id) => <li key={id}>{nameOf(id, mine)}</li>)}
              {!give.length && <li className="scambi-nomi__vuoto">Scegli almeno una tua carta.</li>}
            </ul>
            <span className="scambi-freccia">Ricevi · {get.length}</span>
            <ul className="scambi-nomi">
              {get.map((id) => <li key={id}>{nameOf(id, theirs ?? [])}</li>)}
              {!get.length && <li className="scambi-nomi__vuoto">Scegli almeno una carta di {name}.</li>}
            </ul>
            {draft.replaces && !sent && <p className="m-regola">Inviandola, la proposta che hai ricevuto viene rifiutata.</p>}
            <p className={`m-regola ${sent && isFriend ? 'm-ok' : ''} ${isFriend ? '' : 'm-regola--off'}`}>{status}</p>
            {error && <p className="m-regola m-regola--errore" role="alert">{error}</p>}
            <div className="scambi-azioni">
              <button type="button" className="q-btn q-btn--argento" aria-disabled={!ready || sending || sent} onClick={() => void send()}>
                <span className="q-btn__in">{sent ? 'Proposta inviata' : sending ? 'Invio…' : 'Invia la proposta'}</span>
              </button>
              <button type="button" className="q-btn q-btn--acciaio" onClick={onBack}><span className="q-btn__in">Torna agli scambi</span></button>
            </div>
          </div>
        </section>
      </div>
    </main>
  )
}
