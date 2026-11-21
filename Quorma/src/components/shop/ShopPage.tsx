import { useEffect, useRef, useState } from 'react'
import { factions } from '../../data/factions'
import { ApiError, GENERIC } from '../../lib/api'
import { isDemoSession } from '../../lib/auth'
import { setBalance, useBalance } from '../../lib/balance'
import { getCollection } from '../../lib/cards'
import { buyPack, isUncertain, listPacks, newPurchaseKey, packName, type OpenedPack, type PackType } from '../../lib/shop'
import type { FactionId } from '../../types/deck'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import '../../styles/collection.css'

const FACTION_IDS: FactionId[] = ['artificiali', 'umani']
const QUANTITIES = [1, 5, 10]
const fmt = (n: number) => n.toLocaleString('it-IT')

type Props = { onOpenPacks: (ids: string[]) => void }

// Negozio (design: Pagina-Negozio). Il backend ha una sola valuta di acquisto e nessuna offerta:
// si sceglie busta, fazione e quantità. L'acquisto apre le buste, che restano da rivelare in Collezione.
export default function ShopPage({ onOpenPacks }: Props) {
  const demo = isDemoSession()
  const [packs, setPacks] = useState<PackType[] | null>(null)
  const [loadError, setLoadError] = useState('')
  const [code, setCode] = useState('')
  const [faction, setFaction] = useState<FactionId>('artificiali')
  const [qty, setQty] = useState(1)
  const [buying, setBuying] = useState(false)
  const [error, setError] = useState('')
  const [bought, setBought] = useState<OpenedPack[]>([])
  const credits = useBalance() // ultimo saldo noto: può essere vecchio, quindi a decidere se basta è il backend
  const [uncertain, setUncertain] = useState(false) // l'ultimo acquisto può essere ripreso senza pagare due volte
  const retry = useRef<{ what: string; key: string; left: number } | null>(null) // acquisto dall'esito incerto

  const load = () => {
    setLoadError('')
    listPacks()
      .then((list) => {
        setPacks(list)
        setCode((c) => c || (list.find((p) => p.featured) ?? list[0])?.code || '')
      })
      .catch((err) => setLoadError(err instanceof ApiError ? err.message : GENERIC))
  }
  useEffect(() => {
    if (!demo) load()
  }, [demo])

  const pack = packs?.find((p) => p.code === code) ?? null
  const total = pack ? pack.price * qty : 0
  const short = credits !== null && total > credits

  const buy = async () => {
    if (!pack || buying) return
    setBuying(true)
    setUncertain(false)
    setError('')
    setBought([])
    // Serve solo per dire "nuova" o "doppione": se non arriva, l'acquisto va avanti lo stesso.
    const owned = await getCollection().then((c) => new Set(c.cards.map((x) => x.cardId))).catch(() => null)
    const what = `${pack.code}:${faction}:${qty}`
    // Dopo un esito incerto si riprende da dove ci si era fermati, con la stessa chiave: niente buste in più.
    const resumed = retry.current?.what === what ? retry.current : null
    const count = resumed?.left ?? qty
    const done: OpenedPack[] = []
    try {
      for (let i = 0; i < count; i++) {
        const key = i === 0 && resumed ? resumed.key : newPurchaseKey()
        retry.current = { what, key, left: count - i }
        const r = await buyPack(pack.code, faction, key, owned)
        retry.current = null
        done.push(r.pack)
        setBalance(r.creditsAfter)
      }
    } catch (err) {
      if (isUncertain(err)) setUncertain(true)
      else retry.current = null
      const message = err instanceof ApiError ? err.message : GENERIC
      setError(done.length ? `Acquistate ${done.length} buste su ${count}. ${message}` : message)
    } finally {
      setBought(done)
      setBuying(false)
    }
  }

  const pick = (fn: () => void) => {
    fn()
    setBought([])
    setError('')
  }

  return (
    <main className="page shop-page">
      <CurrencyBar />
      <PageTitle title="NEGOZIO" subtitle="BUSTE E OFFERTE" />

      <div className="shop-corpo">
        <section aria-label="Articoli" className="shop-articoli">
          <div className="coll-chips" role="group" aria-label="Sezione del negozio">
            <span className="m-chip" data-on="true">Buste</span>
            <span className="m-chip m-chip--off" title="In arrivo">Offerte · in arrivo</span>
          </div>
          <div className="q-marker">
            <h2 className="q-marker__title">Buste</h2>
            <span className="q-marker__line" />
            {pack && <span className="coll-nota">{pack.set.name}</span>}
          </div>

          {demo ? (
            <p className="coll-vuoto">In modalità demo il negozio non è disponibile: serve il backend.</p>
          ) : loadError ? (
            <div className="coll-vuoto" role="alert">
              <p>{loadError}</p>
              <button type="button" className="q-btn q-btn--acciaio" onClick={load}><span className="q-btn__in">Riprova</span></button>
            </div>
          ) : !packs ? (
            <p className="coll-vuoto" role="status">Caricamento delle buste…</p>
          ) : packs.length === 0 ? (
            <p className="coll-vuoto">Nessuna busta in vendita al momento.</p>
          ) : (
            <div role="radiogroup" aria-label="Scegli una busta" className="shop-griglia">
              {packs.map((p) => {
                const on = p.code === code
                return (
                  <button key={p.code} type="button" role="radio" aria-checked={on} className="pick" onClick={() => pick(() => setCode(p.code))}>
                    <span className={`q-tela q-tela--arco q-carta ${on ? 'q-acceso' : ''}`}>
                      <span className="q-tela__in">
                        <span className="q-carta__arte n-busta" style={{ backgroundImage: `url(${factions[faction].cover})`, backgroundPosition: factions[faction].coverFocus }}>
                          <img className="n-logo" src="/images/ui/logo-quorma.png" alt="" />
                          <span className="n-fascia">{p.featured ? 'In evidenza' : `${p.size} carte`}</span>
                        </span>
                        <span className="q-divisorio" />
                        <span className="q-carta__targa shop-targa">
                          <span className="q-carta__nome">{packName(p.code)}</span>
                          <span className="q-carta__sotto">{p.size} carte</span>
                          <span className="n-prezzo">{fmt(p.price)} <small>Valuta 2</small></span>
                        </span>
                      </span>
                      <span className="q-borchia q-borchia--n" />
                    </span>
                  </button>
                )
              })}
            </div>
          )}
        </section>

        <section aria-label="Acquisto" className="q-tela shop-acquisto">
          <div className="q-tela__in" aria-live="polite">
            <h2 className="q-elenco__titolo">Acquisto</h2>
            {pack ? (
              <>
                <span className="shop-nome">
                  <b>{packName(pack.code)}</b>
                  <small>{factions[faction].name}</small>
                </span>
                <span className="q-divisorio" />

                <div className="q-elenco" role="radiogroup" aria-label="Fazione della busta">
                  <span className="q-elenco__titolo">Fazione</span>
                  {FACTION_IDS.map((f) => (
                    <button key={f} type="button" role="radio" aria-checked={faction === f} className="q-elenco__voce" onClick={() => pick(() => setFaction(f))}>
                      {factions[f].name}
                    </button>
                  ))}
                </div>

                <div className="q-elenco" role="radiogroup" aria-label="Quantità">
                  <span className="q-elenco__titolo">Quantità</span>
                  <div className="shop-qta">
                    {QUANTITIES.map((n) => (
                      <button key={n} type="button" role="radio" aria-checked={qty === n} className="m-chip" onClick={() => pick(() => setQty(n))}>
                        × {n}
                      </button>
                    ))}
                  </div>
                </div>

                <dl className="shop-conti">
                  <div><dt>Saldo Valuta 2</dt><dd>{credits === null ? 'Noto dopo un acquisto' : fmt(credits)}</dd></div>
                  <div><dt>Totale</dt><dd>− {fmt(total)}</dd></div>
                  {credits !== null && <div className="shop-conti__dopo"><dt>Dopo l'acquisto</dt><dd>{short ? '—' : fmt(credits - total)}</dd></div>}
                </dl>

                {short && <p className="m-regola m-regola--errore">L'ultimo saldo noto non basta per questo acquisto.</p>}
                {error && <p className="m-regola m-regola--errore" role="alert">{error}</p>}

                {bought.length > 0 && !(error && uncertain) ? (
                  <div className="q-tela q-tela--arco q-braci shop-fatto">
                    <div className="q-tela__in">
                      <b>Acquisto completato</b>
                      <span>{bought.length === 1 ? 'La busta è pronta da aprire.' : `${bought.length} buste sono pronte da aprire.`}</span>
                      <button type="button" className="q-btn q-btn--argento" onClick={() => onOpenPacks(bought.map((p) => p.id))}>
                        <span className="q-btn__in">Apri ora</span>
                      </button>
                      <button type="button" className="q-btn q-btn--acciaio" onClick={() => setBought([])}>
                        <span className="q-btn__in">Più tardi</span>
                      </button>
                    </div>
                    <span className="q-borchia q-borchia--n" />
                  </div>
                ) : (
                  <button type="button" className="q-btn q-btn--argento shop-compra" disabled={buying} aria-busy={buying} onClick={buy}>
                    <span className="q-btn__in">{buying ? 'Acquisto…' : error && uncertain ? 'Riprova' : 'Acquista'}</span>
                  </button>
                )}
              </>
            ) : (
              <p className="coll-nota-testo">Scegli una busta.</p>
            )}
          </div>
        </section>
      </div>
    </main>
  )
}
