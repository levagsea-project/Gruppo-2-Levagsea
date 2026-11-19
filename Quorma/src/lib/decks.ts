// Mazzi dal backend: GET /decks, GET /decks/{id}, DELETE /decks/{id}, GET /decks/play-status.
// Qui le risposte del backend diventano i tipi del frontend (types/deck.ts).
import type { Deck, DeckStats, FactionId, PlayStatus } from '../types/deck'
import { translate } from './api'
import { authedRequest } from './auth'

export const DECK_SIZE = 32 // il backend accetta solo mazzi di esattamente 32 carte

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

type DeckCardResponse = {
  cardInstanceId: string
  kind: 'CREATURE' | 'SPELL' | 'EQUIPMENT'
  suit: string | null // VINCOLO, FRATTURA, SPIRA, VESTIGIO (solo per le creature)
}

type DeckResponse = Omit<DeckSummary, 'cardCount'> & { cards: DeckCardResponse[] }

type PlayStatusResponse = {
  canPlay: boolean
  outdatedDefense: string[]
  missingDefense: string[]
  outdatedDecks: { id: string; name: string; faction: string; defense: boolean }[]
}

// ---------- Conversioni ----------
const FACTIONS: Record<string, FactionId> = {
  RESISTENZA_UMANA: 'umani',
  POTENZE_ARTIFICIALI: 'artificiali',
}
const toFaction = (f: string): FactionId => FACTIONS[f] ?? 'umani'

// I quattro semi nell'ordine del regolamento: compaiono anche se il mazzo non ne ha.
const SUITS = ['VINCOLO', 'FRATTURA', 'SPIRA', 'VESTIGIO']

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

function toStats(d: DeckResponse): DeckStats {
  const creatures = d.cards.filter((c) => c.kind === 'CREATURE')
  return {
    creatures: creatures.length,
    spells: d.cards.length - creatures.length,
    suits: SUITS.map((s) => ({
      name: translate(`game.suit.${s.toLowerCase()}`),
      value: creatures.filter((c) => c.suit === s).length,
    })),
  }
}

// ---------- Chiamate ----------
export const listDecks = async () => (await authedRequest<DeckSummary[]>('GET', '/decks')).map(toDeck)

export const getDeckStats = async (id: string) => toStats(await authedRequest<DeckResponse>('GET', `/decks/${id}`))

export const deleteDeck = (id: string) => authedRequest<null>('DELETE', `/decks/${id}`)

export async function getPlayStatus(): Promise<PlayStatus> {
  const s = await authedRequest<PlayStatusResponse>('GET', '/decks/play-status')
  return {
    canPlay: s.canPlay,
    missingDefense: s.missingDefense.map(toFaction),
    outdatedDefense: s.outdatedDefense.map(toFaction),
    outdatedDecks: s.outdatedDecks.map((d) => ({ ...d, faction: toFaction(d.faction) })),
  }
}
