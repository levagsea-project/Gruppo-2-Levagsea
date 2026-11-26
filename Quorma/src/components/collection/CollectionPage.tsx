import { useEffect, useMemo, useState } from 'react'
import { factions } from '../../data/factions'
import type { Page } from '../../data/navItems'
import { ApiError, GENERIC } from '../../lib/api'
import { isDemoSession } from '../../lib/auth'
import { matchesCard, sortCards, type CardSort } from '../../lib/cardFilters'
import { SUITS, SUIT_NAMES, getCollection, rankLabel } from '../../lib/cards'
import type { CardKind, FactionId, OwnedCard, SuitId } from '../../types/deck'
import AbilityList from '../cards/AbilityList'
import CardBack from '../cards/CardBack'
import CardFace from '../cards/CardFace'
import CardStats from '../cards/CardStats'
import CardToolbar, { type FilterGroup } from '../cards/CardToolbar'
import CurrencyBar from '../layout/CurrencyBar'
import PageTitle from '../layout/PageTitle'
import '../../styles/collection.css'

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

type Props = { onNavigate: (page: Page) => void }

// Collezione (design: Pagina-Collezione). Le buste da aprire hanno la loro pagina (PacksPage), dalla voce Buste del menu.
// Il backend restituisce solo le carte possedute, non tutto il catalogo: niente percentuale di completamento
// né carte mancanti. Il filtro "Mostra" lavora quindi sulle copie.
export default function CollectionPage({ onNavigate }: Props) {
  const demo = isDemoSession()
  const [cards, setCards] = useState<OwnedCard[] | null>(null)
  const [error, setError] = useState('')
  const [faction, setFaction] = useState<FactionId | 'tutte'>('tutte')
  const [kind, setKind] = useState<CardKind | 'tutti'>('tutti')
  const [suit, setSuit] = useState<SuitId | 'tutti'>('tutti')
  const [view, setView] = useState<View>('tutte')
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<CardSort>('base')
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
  const found = useMemo(() => entries.filter((e) => matchesCard(e.card, query)), [entries, query]) // ricerca per nome e abilità
  const shown = sortCards(found.filter((e) => passFaction(e) && passKind(e) && passSuit(e) && passView(e)), (e) => e.card, sort)
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

  const resetFilters = () =>
    filter(() => {
      setFaction('tutte')
      setKind('tutti')
      setSuit('tutti')
      setView('tutte')
      setSort('base')
    })

  // Ogni numero dice quante carte resterebbero scegliendo quella voce, con gli altri filtri e la ricerca.
  const filterGroups: FilterGroup[] = [
    {
      id: 'fazione',
      label: 'Fazione',
      value: faction,
      defaultValue: 'tutte',
      onChange: (v) => filter(() => setFaction(v as FactionId | 'tutte')),
      options: (['tutte', 'umani', 'artificiali'] as const).map((f) => ({
        value: f,
        label: f === 'tutte' ? 'Tutte' : factions[f].name,
        count: found.filter((e) => (f === 'tutte' || e.card.faction === f) && passKind(e) && passSuit(e) && passView(e)).length,
      })),
    },
    {
      id: 'tipo',
      label: 'Tipo',
      value: kind,
      defaultValue: 'tutti',
      onChange: (v) =>
        filter(() => {
          setKind(v as CardKind | 'tutti')
          if (v === 'SPELL' || v === 'EQUIPMENT') setSuit('tutti')
        }),
      options: KINDS.map(([k, label]) => ({
        value: k,
        label,
        count: found.filter((e) => (k === 'tutti' || e.card.kind === k) && passFaction(e) && (k === 'SPELL' || k === 'EQUIPMENT' || passSuit(e)) && passView(e)).length,
      })),
    },
    ...(hasSuits
      ? [{
          id: 'seme',
          label: 'Seme',
          value: suit,
          defaultValue: 'tutti',
          onChange: (v: string) => filter(() => setSuit(v as SuitId | 'tutti')),
          options: (['tutti', ...SUITS] as const).map((s) => ({
            value: s,
            label: s === 'tutti' ? 'Tutti' : SUIT_NAMES[s],
            suit: s === 'tutti' ? undefined : s,
            count: found.filter((e) => (s === 'tutti' || e.card.suit === s) && passFaction(e) && passKind(e) && passView(e)).length,
          })),
        }]
      : []),
    {
      id: 'copie',
      label: 'Copie',
      value: view,
      defaultValue: 'tutte',
      onChange: (v) => filter(() => setView(v as View)),
      options: VIEWS.map(([v, label]) => ({
        value: v,
        label,
        count: found.filter((e) => (v === 'tutte' || (v === 'doppioni' ? e.copies > 1 : e.copies === 1)) && passFaction(e) && passKind(e) && passSuit(e)).length,
      })),
    },
  ]

  return (
    <main className="page coll-page">
      <CurrencyBar />
      {/* Su desktop la testata sta in una riga (la pagina non scorre); altrove la testata non esiste (display: contents) */}
      <div className="coll-testa">
        <PageTitle title="COLLEZIONE" subtitle="LE TUE CARTE" />
      </div>

      {demo ? (
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
          <section className="coll-carte" aria-label="Carte">
            <div className="q-marker">
              <h2 className="q-marker__title">Carte</h2>
              <span className="q-marker__line" />
              <span className="coll-nota">{shown.length === entries.length ? `${fmt(entries.length)} diverse · ${fmt(cards.length)} copie` : `${fmt(shown.length)} di ${fmt(entries.length)} carte`}</span>
            </div>
            {entries.length > 0 && (
              <CardToolbar query={query} onQuery={(v) => filter(() => setQuery(v))} sort={sort} onSort={(v) => filter(() => setSort(v))} groups={filterGroups} resultCount={shown.length} onReset={resetFilters} />
            )}
            {shown.length === 0 ? (
              entries.length ? (
                <div className="coll-vuoto">
                  <p>Nessuna carta corrisponde alla ricerca o ai filtri.</p>
                  <button
                    type="button"
                    className="q-btn q-btn--acciaio"
                    onClick={() => {
                      setQuery('')
                      resetFilters()
                    }}
                  >
                    <span className="q-btn__in">Azzera ricerca e filtri</span>
                  </button>
                </div>
              ) : (
                <p className="coll-nota-testo">Non possiedi ancora nessuna carta.</p>
              )
            ) : (
              <div className="coll-griglia">
                {visible.map((e) => {
                  const on = selected?.card.cardId === e.card.cardId
                  const label = e.card.kind === 'CREATURE' ? `${e.card.name}, ${rankLabel(e.card.rank)} di ${SUIT_NAMES[e.card.suit!]}, ATK ${e.card.atk}, DEF ${e.card.def}` : `${e.card.name}, ${KIND_NAME[e.card.kind]}`
                  return (
                    <button key={e.card.cardId} type="button" className={`g-cella ${on ? 'g-cella--on' : ''}`} aria-pressed={on} aria-label={`${label}, ${e.copies} ${e.copies === 1 ? 'copia' : 'copie'}`} onClick={() => select(e.card.cardId)}>
                      <span className="g-faccia">
                        <CardFace card={e.card} sizes="(max-width: 767px) 33vw, 180px" />
                        {e.copies > 1 && <span className="g-copie">×{e.copies}</span>}
                      </span>
                      {e.card.kind === 'CREATURE' && <span className="g-nome" aria-hidden="true" lang={e.card.textLocale}>{e.card.name}</span>}
                      <CardStats card={e.card} />
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
                  <span className="q-giro__faccia q-giro__retro"><CardBack card={selected.card} /></span>
                </span>
              </button>
              <span className="coll-suggerimento">{flipped ? 'Premi per tornare al fronte' : 'Premi la carta per leggere la lore'}</span>
              <AbilityList card={selected.card} />
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
