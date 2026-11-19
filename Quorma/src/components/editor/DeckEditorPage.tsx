import { useEffect, useMemo, useState, type CSSProperties } from 'react'
import type { CardKind, DeckDetail, FactionId, OwnedCard, SuitId } from '../../types/deck'
import { factions } from '../../data/factions'
import { ApiError } from '../../lib/api'
import { CREATURE_LIMITS, DECK_SIZE, SUITS, SUIT_NAMES, getCollection, rankArticle, rankLabel, spellLimits, type Collection } from '../../lib/cards'
import { deckErrorMessages, getDeck, saveDeck } from '../../lib/decks'
import PageTitle from '../layout/PageTitle'
import EditorCard, { type CardState } from './EditorCard'
import '../../styles/editor.css'

type Props = {
  faction: FactionId
  deckId: string | null // null = mazzo nuovo
  defaultName: string // nome proposto per un mazzo nuovo (es. "Mazzo 03")
  onCancel: () => void
  onSaved: (deck: DeckDetail) => void
}

// Una carta scelta per il mazzo: la carta e la copia posseduta che finisce nel mazzo.
type Option = { card: OwnedCard; instanceIds: string[] }

const TYPES: { id: CardKind; label: string }[] = [
  { id: 'CREATURE', label: 'Creature' },
  { id: 'SPELL', label: 'Magie' },
  { id: 'EQUIPMENT', label: 'Equipaggiamenti' },
]

// Una voce per carta (non per copia): nel mazzo entra una copia sola di ogni carta.
function toOptions(cards: OwnedCard[], faction: FactionId): Option[] {
  const byCard = new Map<string, Option>()
  for (const c of cards) {
    if (c.faction !== faction) continue
    const o = byCard.get(c.cardId)
    if (o) o.instanceIds.push(c.instanceId)
    else byCard.set(c.cardId, { card: c, instanceIds: [c.instanceId] })
  }
  const suitIndex = (s: SuitId | null) => (s ? SUITS.indexOf(s) : 9)
  return [...byCard.values()].sort(
    (a, b) =>
      suitIndex(a.card.suit) - suitIndex(b.card.suit) ||
      (a.card.rank ?? 99) - (b.card.rank ?? 99) ||
      a.card.name.localeCompare(b.card.name, 'it'),
  )
}

// Editor del mazzo, dal design "Pagina-Mazzi-Editor": le tue carte a sinistra, il mazzo a destra.
// Le regole (32 carte, creature e magie per fazione, una creatura per rango e seme) si controllano mentre
// componi il mazzo; il backend le ricontrolla al salvataggio.
export default function DeckEditorPage({ faction, deckId, defaultName, onCancel, onSaved }: Props) {
  const [collection, setCollection] = useState<Collection | null>(null)
  const [original, setOriginal] = useState<DeckDetail | null>(null)
  const [loadError, setLoadError] = useState('')
  const [attempt, setAttempt] = useState(0)

  const [name, setName] = useState(deckId ? '' : defaultName)
  const [chosen, setChosen] = useState<Record<string, string>>({}) // cardId → copia scelta
  const [type, setType] = useState<CardKind>('CREATURE')
  const [suit, setSuit] = useState<SuitId | 'tutti'>('tutti')

  const [saving, setSaving] = useState(false)
  const [nameError, setNameError] = useState('')
  const [saveErrors, setSaveErrors] = useState<string[]>([])
  const [initial, setInitial] = useState('') // stato iniziale, per chiedere conferma prima di uscire con modifiche non salvate

  // Carica collezione (e il mazzo, se lo stai modificando).
  useEffect(() => {
    let current = true
    Promise.all([getCollection(), deckId ? getDeck(deckId) : Promise.resolve(null)])
      .then(([col, deck]) => {
        if (!current) return
        setCollection(col)
        setOriginal(deck)
        if (deck) {
          setName(deck.name)
          const picked = Object.fromEntries(deck.cardList.map((c) => [c.cardId, c.instanceId]))
          setChosen(picked)
          setInitial(JSON.stringify([deck.name, picked]))
        } else {
          setInitial(JSON.stringify([defaultName, {}]))
        }
      })
      .catch((err: unknown) => current && setLoadError(err instanceof Error ? err.message : 'Impossibile caricare le carte.'))
    return () => {
      current = false
    }
  }, [deckId, defaultName, attempt])

  // Carte disponibili: quelle della collezione, più quelle del mazzo (se una non fosse più tra le tue).
  const options = useMemo(() => {
    if (!collection) return []
    const extra = original?.cardList.filter((c) => !collection.cards.some((x) => x.instanceId === c.instanceId)) ?? []
    return toOptions([...collection.cards, ...extra], faction)
  }, [collection, original, faction])

  const inDeck = options.filter((o) => chosen[o.card.cardId])
  const creatures = inDeck.filter((o) => o.card.kind === 'CREATURE')
  const total = inDeck.length
  const nC = creatures.length
  const nS = total - nC
  const cLim = CREATURE_LIMITS[faction]
  const sLim = spellLimits(faction)
  const takenSlots = new Map(inDeck.map((o) => [o.card.slot, o.card.cardId]))

  const okTotal = total === DECK_SIZE
  const okCreatures = nC >= cLim.min && nC <= cLim.max
  const okSpells = nS >= sLim.min && nS <= sLim.max
  const valid = okTotal && okCreatures && okSpells && name.trim().length > 0

  const dirty = initial !== '' && initial !== JSON.stringify([name, chosen])

  function stateOf(o: Option): { state: CardState; reason: string } {
    const c = o.card
    if (chosen[c.cardId]) return { state: 'dentro', reason: '' }
    const other = takenSlots.get(c.slot)
    if (other && other !== c.cardId) {
      return { state: 'bloccata', reason: c.kind === 'CREATURE' ? `Hai già ${rankArticle(c.rank)} di ${SUIT_NAMES[c.suit!]}` : 'Già nel mazzo' }
    }
    if (total >= DECK_SIZE) return { state: 'bloccata', reason: 'Mazzo pieno' }
    if (c.kind === 'CREATURE' && nC >= cLim.max) return { state: 'bloccata', reason: `Massimo ${cLim.max} creature` }
    if (c.kind !== 'CREATURE' && nS >= sLim.max) return { state: 'bloccata', reason: `Massimo ${sLim.max} magie/equip.` }
    return { state: 'libera', reason: '' }
  }

  const toggle = (o: Option) => {
    setSaveErrors([])
    setChosen((prev) => {
      const next = { ...prev }
      if (next[o.card.cardId]) delete next[o.card.cardId]
      else next[o.card.cardId] = o.instanceIds[0]
      return next
    })
  }

  async function save() {
    if (!valid || saving || !collection) return
    setSaving(true)
    setNameError('')
    setSaveErrors([])
    // Pool di carte da provare: quello abbinato alla fazione, poi gli altri, poi quello del mazzo.
    const candidates = [collection.snapshotFor[faction], ...collection.snapshotIds, original?.snapshotId].filter(
      (id, i, all): id is string => !!id && all.indexOf(id) === i,
    )
    try {
      const deck = await saveDeck({ id: deckId ?? undefined, name, faction, snapshotIds: candidates, instanceIds: inDeck.map((o) => chosen[o.card.cardId]) })
      setInitial(JSON.stringify([name, chosen]))
      onSaved(deck)
    } catch (err) {
      if (err instanceof ApiError && err.code === 'deck.name_taken') setNameError(err.message)
      else setSaveErrors(deckErrorMessages(err))
      setSaving(false)
    }
  }

  const cancel = () => {
    if (dirty && !window.confirm('Uscire senza salvare? Le modifiche al mazzo andranno perse.')) return
    onCancel()
  }

  const list = options.filter((o) => o.card.kind === type && (type !== 'CREATURE' || suit === 'tutti' || o.card.suit === suit))
  const countOf = (k: CardKind) => inDeck.filter((o) => o.card.kind === k).length
  const loading = !collection && !loadError
  const hasCards = options.length > 0

  const missing = DECK_SIZE - total
  const rules = [
    { ok: okTotal, text: okTotal ? `${DECK_SIZE} carte` : missing > 0 ? (missing === 1 ? 'Manca 1 carta' : `Mancano ${missing} carte`) : `Togli ${-missing} carte` },
    { ok: okCreatures, text: `Creature ${nC} · servono ${cLim.min}–${cLim.max}` },
    { ok: okSpells, text: `Magie ed equipaggiamenti ${nS} · servono ${sLim.min}–${sLim.max}` },
  ]

  return (
    <main className="page editor-page">
      <header className="editor-testa">
        <PageTitle title={deckId ? 'MODIFICA MAZZO' : 'NUOVO MAZZO'} subtitle={factions[faction].name.toUpperCase()} />
        <label className="editor-nome">
          <span className="decks-etichetta">Nome del mazzo</span>
          <input
            className="editor-nome__input"
            type="text"
            maxLength={64}
            value={name}
            onChange={(e) => {
              setName(e.target.value)
              setNameError('')
            }}
            aria-invalid={nameError || !name.trim() ? true : undefined}
            aria-describedby={nameError ? 'nome-err' : undefined}
          />
          {nameError && <span id="nome-err" className="info-errore">{nameError}</span>}
        </label>
      </header>

      {/* Avanzamento, regole e pulsanti */}
      <section className="q-tela" aria-label="Stato del mazzo">
        <div className="q-tela__in editor-barra">
          <span className="m-conta">
            <b>
              {total} <span className="editor-barra__di">/ {DECK_SIZE}</span>
            </b>
            <small>Carte nel mazzo</small>
          </span>
          <div className="editor-barra__regole">
            <span className="q-xp" style={{ '--xp': `${Math.min(100, (total / DECK_SIZE) * 100)}%` } as CSSProperties}>
              <span className="q-xp__bar" role="progressbar" aria-label="Carte nel mazzo" aria-valuenow={total} aria-valuemin={0} aria-valuemax={DECK_SIZE} />
            </span>
            <div className="editor-barra__elenco" aria-live="polite">
              {rules.map((r) => (
                <span key={r.text} className={`m-regola ${r.ok ? 'm-ok' : ''}`}>{r.text}</span>
              ))}
            </div>
          </div>
          <div className="editor-barra__azioni">
            <button type="button" className="q-btn q-btn--acciaio" onClick={cancel}>
              <span className="q-btn__in">Annulla</span>
            </button>
            <button type="button" className="q-btn q-btn--argento" aria-disabled={!valid || saving} onClick={save}>
              <span className="q-btn__in">{saving ? 'Salvataggio…' : 'Salva mazzo'}</span>
            </button>
          </div>
        </div>
      </section>

      {saveErrors.length > 0 && (
        <div className="editor-errori" role="alert">
          <b>Il mazzo non è stato salvato:</b>
          <ul>
            {saveErrors.map((e) => (
              <li key={e}>{e}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="editor-pannello">
        {/* Le tue carte */}
        <section className="editor-carte" aria-labelledby="e-tue">
          <div className="q-marker">
            <h2 id="e-tue" className="q-marker__title">Le tue carte</h2>
            <span className="q-marker__line" aria-hidden="true" />
            <span className="decks-conta">Premi una carta per aggiungerla</span>
          </div>

          <div className="editor-filtri">
            <div className="info-chips" role="group" aria-label="Tipo di carta">
              {TYPES.map((t) => (
                <button key={t.id} type="button" className="info-chip" aria-pressed={type === t.id} onClick={() => setType(t.id)}>
                  {t.label} <span className="editor-filtri__n">{countOf(t.id)}</span>
                </button>
              ))}
            </div>
            {type === 'CREATURE' && (
              <div className="info-chips" role="group" aria-label="Seme">
                <button type="button" className="info-chip" aria-pressed={suit === 'tutti'} onClick={() => setSuit('tutti')}>
                  Tutti
                </button>
                {SUITS.map((s) => (
                  <button key={s} type="button" className="info-chip" aria-pressed={suit === s} onClick={() => setSuit(s)}>
                    <span className={`deck-seme deck-seme--${s.toLowerCase()} editor-filtri__seme`} aria-hidden="true" />
                    {SUIT_NAMES[s]}
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="editor-lista">
            {loading && <p className="deck-empty">Caricamento delle tue carte…</p>}
            {loadError && (
              <p className="decks-avviso decks-avviso--errore" role="alert">
                {loadError}
                <button type="button" onClick={() => { setLoadError(''); setAttempt((n) => n + 1) }}>Riprova</button>
              </p>
            )}
            {collection && !hasCards && (
              <p className="deck-empty">
                Non hai ancora carte della {factions[faction].name}. Le carte si ottengono aprendo le buste del Negozio, che è in arrivo.
              </p>
            )}
            {collection && hasCards && list.length === 0 && <p className="deck-empty">Nessuna carta di questo tipo nella tua collezione.</p>}
            <div className="editor-griglia">
              {list.map((o) => {
                const { state, reason } = stateOf(o)
                return <EditorCard key={o.card.cardId} card={o.card} state={state} reason={reason} onToggle={() => toggle(o)} />
              })}
            </div>
          </div>
        </section>

        {/* Il tuo mazzo */}
        <section className="q-tela editor-mazzo" aria-labelledby="e-mazzo">
          <div className="q-tela__in">
            <h2 id="e-mazzo" className="decks-etichetta">Il tuo mazzo</h2>
            <p className="editor-mazzo__aiuto">Una creatura per ogni rango e seme. Premi un pezzo per toglierlo.</p>

            <span className="editor-freccia">Creature · {nC}</span>
            {SUITS.map((s) => {
              const pieces = creatures.filter((o) => o.card.suit === s)
              return (
                <div key={s} className="editor-sezione">
                  <span className="editor-sezione__testa">
                    <span className={`deck-seme deck-seme--${s.toLowerCase()}`} aria-hidden="true" />
                    {SUIT_NAMES[s]}
                    <small>{pieces.length}</small>
                  </span>
                  <div className="editor-pezzi">
                    {pieces.map((o) => (
                      <Piece key={o.card.cardId} label={rankLabel(o.card.rank)} name={o.card.name} aria={`Togli ${o.card.name}, ${rankLabel(o.card.rank)} di ${SUIT_NAMES[s]}`} onRemove={() => toggle(o)} />
                    ))}
                    {pieces.length === 0 && <span className="editor-vuoto">Nessuna creatura di questo seme.</span>}
                  </div>
                </div>
              )
            })}

            <span className="editor-freccia">Magie ed equipaggiamenti · {nS}</span>
            <div className="editor-sezione">
              <div className="editor-pezzi">
                {inDeck
                  .filter((o) => o.card.kind !== 'CREATURE')
                  .map((o) => (
                    <Piece key={o.card.cardId} label={o.card.kind === 'SPELL' ? 'M' : 'E'} name={o.card.name} aria={`Togli ${o.card.name}`} onRemove={() => toggle(o)} />
                  ))}
                {nS === 0 && <span className="editor-vuoto">Nessuna magia o equipaggiamento.</span>}
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  )
}

// Un "pezzo" del mazzo: rango o M/E, nome, crocetta per toglierlo.
function Piece({ label, name, aria, onRemove }: { label: string; name: string; aria: string; onRemove: () => void }) {
  return (
    <button type="button" className="editor-pezzo" aria-label={aria} onClick={onRemove}>
      {label} <small>{name}</small>
      <svg aria-hidden="true" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round">
        <path d="M6 6l12 12M18 6 6 18" />
      </svg>
    </button>
  )
}
