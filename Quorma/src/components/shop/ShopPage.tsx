import { useEffect, useRef, useState } from 'react'
import { factions } from '../../data/factions'
import { ApiError, GENERIC, translate } from '../../lib/api'
import { isDemoSession } from '../../lib/auth'
import { formatCredits, setBalance, useBalance } from '../../lib/balance'
import { getCollection } from '../../lib/cards'
import { buyPack, isUncertain, listPacks, newPurchaseKey, packName, type OpenedPack, type PackType } from '../../lib/shop'
import type { FactionId } from '../../types/deck'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import SealedPack from './SealedPack'
import '../../styles/collection.css'

const FACTION_IDS: FactionId[] = ['artificiali', 'umani']
const QUANTITIES = [1, 5, 10]
const fmt = formatCredits

// Il rombo della Valuta 2, lo stesso della barra in alto.
const PriceIcon = () => (
  <svg aria-hidden="true" width="12" height="12" viewBox="0 0 24 24" fill="rgba(74,166,173,0.25)" stroke="var(--petrol-500)" strokeWidth="2" strokeLinejoin="round">
    <path d="M12 2 21 12l-9 10-9-10z" />
  </svg>
)

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
          <div className="q-marker shop-testa">
            <h2 className="sr-only">Buste</h2>
            <div className="coll-chips" role="group" aria-label="Sezione del negozio">
              <span className="m-chip" data-on="true">Buste</span>
              <span className="m-chip m-chip--off" title="In arrivo">Offerte · in arrivo</span>
            </div>
            <span className="q-marker__line" />
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
            <div className="shop-matrice">
              <div role="radiogroup" aria-label="Scegli una busta" className="shop-fazioni">
                {FACTION_IDS.map((f) => (
                  <div key={f} className="shop-fazione">
                    <h3 className="shop-fazione__nome">{factions[f].name}</h3>
                    <div className="shop-griglia">
                      {packs.map((p) => {
                        const on = p.code === code && f === faction
                        return (
                          <button
                            key={p.code}
                            type="button"
                            role="radio"
                            aria-checked={on}
                            aria-label={`${packName(p.code)}, ${factions[f].name}: ${p.size} carte, ${fmt(p.price)} Valuta 2${p.featured ? ', in evidenza' : ''}`}
                            className="pick shop-busta"
                            onClick={() =>
                              pick(() => {
                                setCode(p.code)
                                setFaction(f)
                              })
                            }
                          >
                            <SealedPack code={p.code} faction={f} size={p.size} setName={p.set.name} featured={p.featured} />
                            <span className="shop-busta__nome">{packName(p.code).replace(/^Pacchetto\s+/i, '')}</span>
                            <span className="n-prezzo"><PriceIcon />{fmt(p.price)}</span>
                          </button>
                        )
                      })}
                    </div>
                  </div>
                ))}
              </div>
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
                <div className="shop-blocco">
                  <span className="q-elenco__titolo">Contiene {pack.size} carte</span>
                  <ul className="shop-contenuto">
                    {Object.entries(pack.slots).filter(([, n]) => n > 0).map(([tier, n]) => (
                      <li key={tier}><span>{translate(`game.tier.${tier.toLowerCase()}`)}</span><b>{n}</b></li>
                    ))}
                    {pack.supportSlots > 0 && <li><span>Magie o equipaggiamenti</span><b>{pack.supportSlots}</b></li>}
                  </ul>
                </div>

                <div className="shop-cassa">
                  <div className="shop-blocco shop-blocco--qta">
                    <span className="q-elenco__titolo">Quantità</span>
                    <div className="shop-qta" role="radiogroup" aria-label="Quantità">
                      {QUANTITIES.map((n) => (
                        <button key={n} type="button" role="radio" aria-checked={qty === n} className="m-chip" onClick={() => pick(() => setQty(n))}>
                          × {n}
                        </button>
                      ))}
                    </div>
                  </div>

                  <dl className="shop-conti">
                    <div><dt>Saldo</dt><dd>{credits === null ? 'Noto dopo un acquisto' : fmt(credits)}</dd></div>
                    <div><dt>Totale</dt><dd>− {fmt(total)}</dd></div>
                    {credits !== null && <div className="shop-conti__dopo"><dt>Dopo l'acquisto</dt><dd>{short ? '—' : fmt(credits - total)}</dd></div>}
                  </dl>

                  {short && <p className="m-regola m-regola--errore">L'ultimo saldo noto non basta per questo acquisto.</p>}
                  {error && <p className="m-regola m-regola--errore" role="alert">{error}</p>}

                  {bought.length > 0 && !(error && uncertain) ? (
                    <div className="shop-fatto">
                      <b>Acquisto completato</b>
                      <span>{bought.length === 1 ? 'La busta è pronta da aprire.' : `${bought.length} buste sono pronte da aprire.`}</span>
                      <div className="shop-fatto__azioni">
                        <button type="button" className="q-btn q-btn--acciaio" onClick={() => setBought([])}>
                          <span className="q-btn__in">Più tardi</span>
                        </button>
                        <button type="button" className="q-btn q-btn--argento" onClick={() => onOpenPacks(bought.map((p) => p.id))}>
                          <span className="q-btn__in">Apri ora</span>
                        </button>
                      </div>
                    </div>
                  ) : (
                    <button type="button" className="q-btn q-btn--argento shop-compra" disabled={buying} aria-busy={buying} onClick={buy}>
                      <span className="q-btn__in">{buying ? 'Acquisto…' : error && uncertain ? 'Riprova' : 'Acquista'}</span>
                    </button>
                  )}
                </div>
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
