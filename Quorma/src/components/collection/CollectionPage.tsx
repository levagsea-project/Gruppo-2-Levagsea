import { useEffect, useMemo, useState } from 'react'
import { factions } from '../../data/factions'
import type { Page } from '../../data/navItems'
import { ApiError, GENERIC } from '../../lib/api'
import { isDemoSession } from '../../lib/auth'
import { SUITS, SUIT_NAMES, getCollection, rankLabel } from '../../lib/cards'
import { packName, pendingPacks, type OpenedPack } from '../../lib/shop'
import type { CardKind, FactionId, OwnedCard, SuitId } from '../../types/deck'
import CardFace from '../cards/CardFace'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import SealedPack from '../shop/SealedPack'
import '../../styles/collection.css'

export type CollectionTab = 'carte' | 'buste'
type Entry = { card: OwnedCard; copies: number }
type View = 'tutte' | 'doppioni' | 'singole'

const PAGE_SIZE = 12
const KIND_ORDER = { CREATURE: 0, SPELL: 1, EQUIPMENT: 2 }
const KIND_NAME = { CREATURE: 'Creatura', SPELL: 'Magia', EQUIPMENT: 'Equipaggiamento' }
const KINDS: [CardKind | 'tutti', string][] = [['tutti', 'Tutti'], ['CREATURE', 'Creature'], ['SPELL', 'Magie'], ['EQUIPMENT', 'Equipaggiamenti']]
const VIEWS: [View, string][] = [['tutte', 'Tutte'], ['doppioni', 'Doppioni'], ['singole', 'Copia singola']]
const fmt = (n: number | null) => (n === null ? '–' : n.toLocaleString('it-IT'))
// L'Asso è la carta più alta: in elenco viene dopo il King.
const rankOrder = (r: number | null) => (r === 1 ? 14 : r ?? 0)

function group(cards: OwnedCard[]): Entry[] {
  const byId = new Map<string, Entry>()
  for (const card of cards) {
    const e = byId.get(card.cardId)
    if (e) e.copies++
    else byId.set(card.cardId, { card, copies: 1 })
  }
  return [...byId.values()].sort(
    (a, b) =>
      a.card.faction.localeCompare(b.card.faction) ||
      KIND_ORDER[a.card.kind] - KIND_ORDER[b.card.kind] ||
      (a.card.suit ?? '').localeCompare(b.card.suit ?? '') ||
      rankOrder(a.card.rank) - rankOrder(b.card.rank) ||
      a.card.name.localeCompare(b.card.name, 'it'),
  )
}

type Props = { initialTab: CollectionTab; onNavigate: (page: Page) => void; onOpenPacks: (ids: string[]) => void }

// Collezione (design: Pagina-Collezione e Pagina-Collezione-Buste).
// Il backend restituisce solo le carte possedute, non tutto il catalogo: niente percentuale di completamento
// né carte mancanti. Il filtro "Mostra" lavora quindi sulle copie.
export default function CollectionPage({ initialTab, onNavigate, onOpenPacks }: Props) {
  const demo = isDemoSession()
  const [tab, setTab] = useState<CollectionTab>(initialTab)
  const [cards, setCards] = useState<OwnedCard[] | null>(null)
  const [error, setError] = useState('')
  const [faction, setFaction] = useState<FactionId | 'tutte'>('tutte')
  const [kind, setKind] = useState<CardKind | 'tutti'>('tutti')
  const [suit, setSuit] = useState<SuitId | 'tutti'>('tutti')
  const [view, setView] = useState<View>('tutte')
  const [pageN, setPageN] = useState(0)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [detailOpen, setDetailOpen] = useState(false) // su telefono il dettaglio è una schermata a parte
  const [flipped, setFlipped] = useState(false)

  const load = () => {
    setError('')
    getCollection()
      .then((c) => setCards(c.cards))
      .catch((err) => setError(err instanceof ApiError ? err.message : GENERIC))
  }
  useEffect(() => {
    if (!demo) load()
  }, [demo])

  const entries = useMemo(() => group(cards ?? []), [cards])
  const passFaction = (e: Entry) => faction === 'tutte' || e.card.faction === faction
  const passSuit = (e: Entry) => suit === 'tutti' || e.card.suit === suit
  const passView = (e: Entry) => view === 'tutte' || (view === 'doppioni' ? e.copies > 1 : e.copies === 1)
  const passKind = (e: Entry) => kind === 'tutti' || e.card.kind === kind
  const hasSuits = kind === 'tutti' || kind === 'CREATURE' // magie ed equipaggiamenti non hanno seme
  const shown = entries.filter((e) => passFaction(e) && passKind(e) && passSuit(e) && passView(e))
  const pages = Math.max(1, Math.ceil(shown.length / PAGE_SIZE))
  const current = Math.min(pageN, pages - 1)
  const visible = shown.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE)
  const selected = shown.find((e) => e.card.cardId === selectedId) ?? visible[0] ?? null

  const filter = (fn: () => void) => {
    fn()
    setPageN(0)
    setFlipped(false)
  }
  const select = (id: string) => {
    setSelectedId(id)
    setFlipped(false)
    setDetailOpen(true)
  }

  const pending = pendingPacks()

  return (
    <main className="page coll-page">
      <CurrencyBar />
      <PageTitle title="COLLEZIONE" subtitle={tab === 'carte' ? 'LE TUE CARTE' : 'BUSTE DA APRIRE'} />
      <div className="coll-chips" role="radiogroup" aria-label="Sezioni della collezione">
        <button type="button" role="radio" className="m-chip" aria-checked={tab === 'carte'} onClick={() => setTab('carte')}>Carte</button>
        <button type="button" role="radio" className="m-chip" aria-checked={tab === 'buste'} onClick={() => setTab('buste')}>
          Buste da aprire{pending.length > 0 && <span className="m-chip__n">{pending.length}</span>}
        </button>
      </div>

      {tab === 'buste' ? (
        <PacksTab packs={pending} onOpen={onOpenPacks} onShop={() => onNavigate('negozio')} />
      ) : demo ? (
        <p className="coll-vuoto">In modalità demo la collezione non è disponibile: serve il backend.</p>
      ) : error ? (
        <div className="coll-vuoto" role="alert">
          <p>{error}</p>
          <button type="button" className="q-btn q-btn--acciaio" onClick={load}><span className="q-btn__in">Riprova</span></button>
        </div>
      ) : !cards ? (
        <p className="coll-vuoto" role="status">Caricamento della collezione…</p>
      ) : (
        <div className="coll-corpo">
          <aside className="coll-filtri" aria-label="Filtri della collezione">
            <div className="coll-totali">
              <span className="m-conta"><b>{fmt(entries.length)}</b><small>Carte diverse</small></span>
              <span className="m-conta"><b>{fmt(cards.length)}</b><small>Copie</small></span>
            </div>
            <div className="q-elenco" role="radiogroup" aria-label="Fazione">
              <span className="q-elenco__titolo">Fazione</span>
              {(['tutte', 'umani', 'artificiali'] as const).map((f) => (
                <button key={f} type="button" role="radio" aria-checked={faction === f} className="q-elenco__voce" onClick={() => filter(() => setFaction(f))}>
                  {f === 'tutte' ? 'Tutte' : factions[f].name}
                  <span className="q-elenco__conta">{entries.filter((e) => (f === 'tutte' || e.card.faction === f) && passKind(e) && passSuit(e) && passView(e)).length}</span>
                </button>
              ))}
            </div>
            <div className="q-elenco" role="radiogroup" aria-label="Tipo di carta">
              <span className="q-elenco__titolo">Tipo</span>
              {KINDS.map(([k, label]) => (
                <button
                  key={k}
                  type="button"
                  role="radio"
                  aria-checked={kind === k}
                  className="q-elenco__voce"
                  onClick={() =>
                    filter(() => {
                      setKind(k)
                      if (k === 'SPELL' || k === 'EQUIPMENT') setSuit('tutti')
                    })
                  }
                >
                  {label}
                  <span className="q-elenco__conta">{entries.filter((e) => (k === 'tutti' || e.card.kind === k) && passFaction(e) && (k === 'SPELL' || k === 'EQUIPMENT' || passSuit(e)) && passView(e)).length}</span>
                </button>
              ))}
            </div>
            {hasSuits && (
              <div className="q-elenco" role="radiogroup" aria-label="Seme">
                <span className="q-elenco__titolo">Seme</span>
                {(['tutti', ...SUITS] as const).map((s) => (
                  <button key={s} type="button" role="radio" aria-checked={suit === s} className="q-elenco__voce" onClick={() => filter(() => setSuit(s))}>
                    {s === 'tutti' ? <span className="q-elenco__ico" /> : <span className={`deck-seme deck-seme--${s.toLowerCase()} q-elenco__ico`} aria-hidden="true" />}
                    {s === 'tutti' ? 'Tutti' : SUIT_NAMES[s]}
                    <span className="q-elenco__conta">{entries.filter((e) => (s === 'tutti' || e.card.suit === s) && passFaction(e) && passKind(e) && passView(e)).length}</span>
                  </button>
                ))}
              </div>
            )}
            <div className="q-elenco" role="radiogroup" aria-label="Mostra">
              <span className="q-elenco__titolo">Mostra</span>
              {VIEWS.map(([v, label]) => (
                <button key={v} type="button" role="radio" aria-checked={view === v} className="q-elenco__voce" onClick={() => filter(() => setView(v))}>
                  {label}
                  <span className="q-elenco__conta">{entries.filter((e) => (v === 'tutte' || (v === 'doppioni' ? e.copies > 1 : e.copies === 1)) && passFaction(e) && passKind(e) && passSuit(e)).length}</span>
                </button>
              ))}
            </div>
          </aside>

          <section className="coll-carte" aria-label="Carte">
            <div className="q-marker">
              <h2 className="q-marker__title">Carte</h2>
              <span className="q-marker__line" />
              <span className="coll-nota">{shown.length} risultati</span>
            </div>
            {shown.length === 0 ? (
              <p className="coll-nota-testo">{entries.length ? 'Nessuna carta con questi filtri.' : 'Non possiedi ancora nessuna carta.'}</p>
            ) : (
              <div className="coll-griglia">
                {visible.map((e) => {
                  const on = selected?.card.cardId === e.card.cardId
                  const label = e.card.kind === 'CREATURE' ? `${e.card.name}, ${rankLabel(e.card.rank)} di ${SUIT_NAMES[e.card.suit!]}` : `${e.card.name}, ${KIND_NAME[e.card.kind]}`
                  return (
                    <button key={e.card.cardId} type="button" className={`g-cella ${on ? 'g-cella--on' : ''}`} aria-pressed={on} aria-label={`${label}, ${e.copies} ${e.copies === 1 ? 'copia' : 'copie'}`} onClick={() => select(e.card.cardId)}>
                      <CardFace card={e.card} sizes="(max-width: 767px) 33vw, 180px" />
                      {e.copies > 1 && <span className="g-copie">×{e.copies}</span>}
                    </button>
                  )
                })}
              </div>
            )}
            {pages > 1 && (
              <nav className="coll-pagine" aria-label="Pagine">
                <button type="button" className="g-pagina" disabled={current === 0} aria-label="Pagina precedente" onClick={() => setPageN(current - 1)}>‹</button>
                <span>Pagina <b>{current + 1}</b> / {pages}</span>
                <button type="button" className="g-pagina" disabled={current === pages - 1} aria-label="Pagina successiva" onClick={() => setPageN(current + 1)}>›</button>
              </nav>
            )}
          </section>

          {selected && (
            <section className={`coll-dettaglio ${detailOpen ? 'coll-dettaglio--aperto' : ''}`} aria-label="Carta scelta">
              <div className="coll-dettaglio__testa">
                <h2 className="q-elenco__titolo">Dettagli</h2>
                <button type="button" className="coll-chiudi" onClick={() => setDetailOpen(false)}>Chiudi</button>
              </div>
              <button type="button" className="q-giro coll-giro" aria-pressed={flipped} aria-label={flipped ? `Torna al fronte di ${selected.card.name}` : `Gira ${selected.card.name} per leggere la lore`} onClick={() => setFlipped((v) => !v)}>
                <span className="q-giro__dentro">
                  <span className="q-giro__faccia"><CardFace card={selected.card} sizes="300px" /></span>
                  <span className="q-giro__faccia q-giro__retro q-cg">
                    <span className="q-cg__campo q-cg__targa coll-retro__nome">
                      <span className="q-cg__nome" lang={selected.card.textLocale}>{selected.card.name}</span>
                      <span className="q-cg__sotto">Lore</span>
                    </span>
                    <span className="q-cg__campo coll-retro__testo">
                      <span className="g-lore" lang={selected.card.textLocale}>{selected.card.flavour ?? 'Questa carta non ha ancora un testo.'}</span>
                    </span>
                    <span className="q-cg__campo coll-retro__logo"><img src="/images/ui/logo-quorma.png" alt="" /></span>
                  </span>
                </span>
              </button>
              <span className="coll-suggerimento">{flipped ? 'Premi per tornare al fronte' : 'Premi la carta per leggere la lore'}</span>
              <dl className="coll-info">
                <div><dt>Fazione</dt><dd>{factions[selected.card.faction].name}</dd></div>
                <div><dt>Tipo</dt><dd>{KIND_NAME[selected.card.kind]}</dd></div>
                {selected.card.sets[0] && <div><dt>Set</dt><dd>{selected.card.sets[0].name}</dd></div>}
                <div><dt>Copie</dt><dd>{selected.copies}</dd></div>
              </dl>
              <button type="button" className="q-btn q-btn--acciaio" onClick={() => onNavigate('mazzi')}>
                <span className="q-btn__in">Vai ai mazzi</span>
              </button>
            </section>
          )}
        </div>
      )}
    </main>
  )
}

// Le buste comprate e non ancora rivelate, raggruppate per tipo e fazione.
function PacksTab({ packs, onOpen, onShop }: { packs: OpenedPack[]; onOpen: (ids: string[]) => void; onShop: () => void }) {
  const groups = useMemo(() => {
    const map = new Map<string, { key: string; packType: string; faction: FactionId; size: number; setName?: string; ids: string[] }>()
    for (const p of packs) {
      const key = `${p.packType}:${p.faction}`
      const g = map.get(key)
      if (g) g.ids.push(p.id)
      else map.set(key, { key, packType: p.packType, faction: p.faction, size: p.cards.length, setName: p.cards[0]?.sets[0]?.name, ids: [p.id] })
    }
    return [...map.values()]
  }, [packs])
  const [selKey, setSelKey] = useState<string | null>(null)
  const sel = groups.find((g) => g.key === selKey) ?? groups[0] ?? null

  if (!sel) {
    return (
      <div className="coll-vuoto">
        <p>Non hai buste da aprire.</p>
        <button type="button" className="q-btn q-btn--argento" onClick={onShop}><span className="q-btn__in">Vai al negozio</span></button>
      </div>
    )
  }

  return (
    <div className="shop-corpo">
      <section className="shop-articoli" aria-label="Le tue buste">
        <div className="q-marker">
          <h2 className="q-marker__title">Le tue buste</h2>
          <span className="q-marker__line" />
          <span className="coll-nota">{packs.length} da aprire</span>
        </div>
        <div role="radiogroup" aria-label="Scegli le buste da aprire" className="buste-griglia">
          {groups.map((g) => {
            const on = g.key === sel.key
            const f = factions[g.faction]
            return (
              <button key={g.key} type="button" role="radio" aria-checked={on} aria-label={`${packName(g.packType)} ${f.name}: ${g.size} carte, ${g.ids.length} da aprire`} className="pick shop-busta" onClick={() => setSelKey(g.key)}>
                <SealedPack code={g.packType} faction={g.faction} size={g.size} setName={g.setName} count={g.ids.length} />
                <span className="shop-busta__nome">{packName(g.packType).replace(/^Pacchetto\s+/i, '')}</span>
                <span className="shop-busta__fazione">{f.name}</span>
              </button>
            )
          })}
        </div>
      </section>

      <section className="q-tela shop-acquisto" aria-label="Apertura">
        <div className="q-tela__in" aria-live="polite">
          <h2 className="q-elenco__titolo">Buste scelte</h2>
          <span className="shop-nome">
            <b>{packName(sel.packType)}</b>
            <small>{factions[sel.faction].name}</small>
          </span>
          <span className="q-divisorio" />
          <div className="coll-totali">
            <span className="m-conta"><b>{sel.ids.length}</b><small>Da aprire</small></span>
            <span className="m-conta"><b>Negozio</b><small>Provenienza</small></span>
          </div>
          <p className="coll-nota-testo">Le carte sono già nella tua collezione. Si rivelano una alla volta: premile per girarle, oppure rivelale tutte insieme.</p>
          <div className="shop-azioni">
            <button type="button" className="q-btn q-btn--argento" onClick={() => onOpen([sel.ids[0]])}><span className="q-btn__in">Apri una</span></button>
            <button type="button" className="q-btn q-btn--acciaio" onClick={() => onOpen(sel.ids)}>
              <span className="q-btn__in">{sel.ids.length > 1 ? `Apri tutte (${sel.ids.length})` : 'Apri tutte'}</span>
            </button>
          </div>
        </div>
      </section>
    </div>
  )
}
