// Carte: conversione dalle risposte del backend, regole del mazzo e collezione (GET /collection).
import type { CardKind, FactionId, OwnedCard, SuitId } from '../types/deck'
import { authedRequest } from './auth'

// ---------- Fazioni: nomi del backend <-> nomi del frontend ----------
const TO_FRONT: Record<string, FactionId> = { RESISTENZA_UMANA: 'umani', POTENZE_ARTIFICIALI: 'artificiali' }
export const toFaction = (f: string): FactionId => TO_FRONT[f] ?? 'umani'
export const toBackendFaction = (f: FactionId) => (f === 'umani' ? 'RESISTENZA_UMANA' : 'POTENZE_ARTIFICIALI')

// ---------- Regole del mazzo (regolamento 3.1, controllate anche dal backend) ----------
export const DECK_SIZE = 32
export const CREATURE_LIMITS: Record<FactionId, { min: number; max: number }> = {
  umani: { min: 20, max: 23 },
  artificiali: { min: 24, max: 27 },
}
/** Magie ed equipaggiamenti: quello che resta delle 32 carte. */
export const spellLimits = (f: FactionId) => ({ min: DECK_SIZE - CREATURE_LIMITS[f].max, max: DECK_SIZE - CREATURE_LIMITS[f].min })

// ---------- Semi e ranghi ----------
export const SUITS: SuitId[] = ['VINCOLO', 'FRATTURA', 'SPIRA', 'VESTIGIO']
export const SUIT_NAMES: Record<SuitId, string> = { VINCOLO: 'Vincolo', FRATTURA: 'Frattura', SPIRA: 'Spira', VESTIGIO: 'Vestigio' }
const RANKS: Record<number, string> = { 1: 'A', 11: 'J', 12: 'Q', 13: 'K' }
/** Rango da mostrare: 1 → "A", 11 → "J", 7 → "7". */
export const rankLabel = (rank: number | null) => (rank === null ? '' : RANKS[rank] ?? String(rank))
/** Tier di rarità dal rango (regolamento 3.2.2): 2-6 → 1, 7-10 → 2, J/Q/K → 3, Asso → 4. */
export const creatureTier = (rank: number | null): 1 | 2 | 3 | 4 =>
  rank === 1 ? 4 : rank !== null && rank >= 11 ? 3 : rank !== null && rank >= 7 ? 2 : 1
/** "l'Asso", "il 7", "il Jack"… per frasi come "Hai già il 7 di Vincolo". */
export const rankArticle = (rank: number | null) =>
  rank === 1 ? 'l’Asso' : rank === 11 ? 'il Jack' : rank === 12 ? 'la Queen' : rank === 13 ? 'il King' : `il ${rank}`

// ---------- Dal backend ----------
// Una carta posseduta, come la restituiscono GET /collection (CollectionCardResponse) e GET /decks/{id}
// (DeckCardResponse). Nome, testo di colore e set sono nella lingua di Accept-Language (lib/api.ts).
export type CardResponse = {
  cardInstanceId: string
  cardId: string
  cardCode: string
  faction?: string // c'è in GET /collection; nei mazzi è quella del mazzo
  kind: CardKind
  name: string // sempre presente
  flavour: string | null // testo di colore; null se la carta non ne ha
  textLocale: string // lingua di name e flavour
  sets: { code: string; name: string }[]
  subfaction: string | null
  suit: string | null
  rank: number | null
  atk: number | null
  def: number | null
  abilities: unknown[] // abilità con i loro parametri: dati del motore, senza un testo da mostrare (per ora non usate)
  originType?: string // solo in GET /collection: da dove viene la copia (es. STARTER = mazzo iniziale)
  acquiredAt?: string // solo in GET /collection
  deckSlot?: string // solo nei mazzi: il posto nel mazzo (lo calcoliamo anche noi, slotOf)
}

/** Posto nel mazzo, come lo calcola il backend: "C:VINCOLO/3" per le creature, "N:<codice>" per le altre. */
const slotOf = (c: CardResponse) => (c.kind === 'CREATURE' ? `C:${c.suit}/${c.rank}` : `N:${c.cardCode}`)

export function toOwnedCard(c: CardResponse, faction?: FactionId): OwnedCard {
  return {
    instanceId: c.cardInstanceId,
    cardId: c.cardId,
    code: c.cardCode,
    faction: faction ?? toFaction(c.faction ?? ''),
    kind: c.kind,
    name: c.name,
    subfaction: c.subfaction,
    suit: (c.suit as SuitId | null) ?? null,
    rank: c.rank,
    atk: c.atk,
    def: c.def,
    slot: slotOf(c), // calcolato qui come nel backend, uguale per collezione e mazzi
    flavour: c.flavour ?? null,
    textLocale: c.textLocale ?? 'it',
    sets: c.sets ?? [],
  }
}

// ---------- Collezione ----------
type CollectionResponse = { snapshotIds: string[]; cards: CardResponse[] }

export type Collection = {
  cards: OwnedCard[]
  // Pool di carte più recente di ogni fazione. Il backend dà solo gli id, nell'ordine delle sue fazioni
  // (Potenze Artificiali, poi Resistenza Umana) e solo per le fazioni di cui possiedi carte: ricaviamo così
  // l'abbinamento. Il salvataggio del mazzo riprova con l'altro id se il backend lo rifiuta (vedi decks.ts).
  snapshotIds: string[]
  snapshotFor: Partial<Record<FactionId, string>>
}

const BACKEND_ORDER = ['POTENZE_ARTIFICIALI', 'RESISTENZA_UMANA']

export async function getCollection(): Promise<Collection> {
  const r = await authedRequest<CollectionResponse>('GET', '/collection')
  const present = BACKEND_ORDER.filter((f) => r.cards.some((c) => c.faction === f))
  const snapshotFor: Partial<Record<FactionId, string>> = {}
  if (present.length === r.snapshotIds.length) present.forEach((f, i) => (snapshotFor[toFaction(f)] = r.snapshotIds[i]))
  return { cards: r.cards.map((c) => toOwnedCard(c)), snapshotIds: r.snapshotIds, snapshotFor }
}
