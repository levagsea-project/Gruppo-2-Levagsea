// Mazzi dal backend: GET /decks, GET /decks/{id}, POST /decks, PUT /decks/{id}, DELETE /decks/{id},
// POST /decks/{id}/defense, GET /decks/play-status. Qui le risposte diventano i tipi del frontend (types/deck.ts).
import type { Deck, DeckDetail, DeckStats, FactionId, OwnedCard, PlayStatus } from '../types/deck'
import { ApiError, translate, type FieldError } from './api'
import { authedRequest } from './auth'
import { DECK_SIZE, SUITS, SUIT_NAMES, rankLabel, toBackendFaction, toFaction, toOwnedCard, type CardResponse } from './cards'

export { DECK_SIZE }

// ---------- Forma delle risposte del backend ----------
type DeckSummary = {
  id: string
  name: string
  faction: string
  snapshotId: string
  defense: boolean
  outdated: boolean
  cardCount: number
  updatedAt: string
}

type DeckResponse = Omit<DeckSummary, 'cardCount'> & { cards: CardResponse[] }

type PlayStatusResponse = {
  canPlay: boolean
  outdatedDefense: string[]
  missingDefense: string[]
  outdatedDecks: { id: string; name: string; faction: string; defense: boolean }[]
}

// ---------- Conversioni ----------
function toDeck(d: DeckSummary): Deck {
  return {
    id: d.id,
    name: d.name,
    faction: toFaction(d.faction),
    cards: d.cardCount,
    maxCards: DECK_SIZE,
    defense: d.defense,
    outdated: d.outdated,
  }
}

function toDetail(d: DeckResponse): DeckDetail {
  const faction = toFaction(d.faction)
  return {
    id: d.id,
    name: d.name,
    faction,
    cards: d.cards.length,
    maxCards: DECK_SIZE,
    defense: d.defense,
    outdated: d.outdated,
    snapshotId: d.snapshotId,
    cardList: d.cards.map((c) => toOwnedCard(c, faction)),
  }
}

/** Conteggi di un elenco di carte: creature, magie/equipaggiamenti e creature per seme. */
export function statsOf(cards: Pick<OwnedCard, 'kind' | 'suit'>[]): DeckStats {
  const creatures = cards.filter((c) => c.kind === 'CREATURE')
  return {
    creatures: creatures.length,
    spells: cards.length - creatures.length,
    suits: SUITS.map((s) => ({ name: SUIT_NAMES[s], value: creatures.filter((c) => c.suit === s).length })),
  }
}

// ---------- Errori di un mazzo non valido (400 deck.invalid con l'elenco delle regole violate) ----------
function errorText(e: FieldError): string {
  const params = { ...e.params }
  // "C:VINCOLO/3" → "3 di Vincolo"
  if (e.code === 'deck.duplicate_slot' && typeof params.slot === 'string') {
    const m = /^C:(\w+)\/(\d+)$/.exec(params.slot)
    if (m) params.slot = `${rankLabel(Number(m[2]))} di ${SUIT_NAMES[m[1] as keyof typeof SUIT_NAMES] ?? m[1]}`
  }
  return translate(e.code, params)
}

/** Le frasi da mostrare per un errore di salvataggio: una per regola violata, oppure il messaggio generale. */
export function deckErrorMessages(err: unknown): string[] {
  if (!(err instanceof ApiError)) return [translate(null)]
  if (err.errors.length) return [...new Set(err.errors.map(errorText))]
  return [err.message]
}

// ---------- Chiamate ----------
export const listDecks = async () => (await authedRequest<DeckSummary[]>('GET', '/decks')).map(toDeck)

export const getDeck = async (id: string) => toDetail(await authedRequest<DeckResponse>('GET', `/decks/${id}`))

export const getDeckStats = async (id: string) => statsOf((await getDeck(id)).cardList)

export const deleteDeck = (id: string) => authedRequest<null>('DELETE', `/decks/${id}`)

export const setDefenseDeck = async (id: string) => toDetail(await authedRequest<DeckResponse>('POST', `/decks/${id}/defense`))

type SaveInput = {
  id?: string // assente = mazzo nuovo
  name: string
  faction: FactionId
  snapshotIds: string[] // pool candidati, il primo è quello più probabile (vedi Collection in cards.ts)
  instanceIds: string[]
}

const isSnapshotError = (err: unknown) => err instanceof ApiError && err.errors.some((e) => e.code === 'deck.snapshot_invalid')

/**
 * Crea (POST) o aggiorna (PUT) un mazzo. Il backend vuole l'id del pool di carte della fazione: se rifiuta il
 * primo candidato perché è di un'altra fazione (deck.snapshot_invalid), riprova con il successivo.
 */
export async function saveDeck({ id, name, faction, snapshotIds, instanceIds }: SaveInput): Promise<DeckDetail> {
  let lastError: unknown = new ApiError(400, 'deck.snapshot_invalid', translate('deck.snapshot_invalid'))
  for (const snapshotId of snapshotIds) {
    try {
      const body = { name: name.trim(), snapshotId, cardInstanceIds: instanceIds }
      const r = id
        ? await authedRequest<DeckResponse>('PUT', `/decks/${id}`, body)
        : await authedRequest<DeckResponse>('POST', '/decks', { ...body, faction: toBackendFaction(faction) })
      return toDetail(r)
    } catch (err) {
      lastError = err
      if (!isSnapshotError(err)) break
    }
  }
  throw lastError
}

export async function getPlayStatus(): Promise<PlayStatus> {
  const s = await authedRequest<PlayStatusResponse>('GET', '/decks/play-status')
  return {
    canPlay: s.canPlay,
    missingDefense: s.missingDefense.map(toFaction),
    outdatedDefense: s.outdatedDefense.map(toFaction),
    outdatedDecks: s.outdatedDecks.map((d) => ({ ...d, faction: toFaction(d.faction) })),
  }
}
