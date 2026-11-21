// Negozio e buste (GET /packs, POST /packs/open).
// Il backend apre la busta nel momento dell'acquisto: le carte sono subito nella collezione.
// "Buste da aprire" è quindi solo la rivelazione ancora da guardare: la teniamo nel browser, per account.
// Se il browser la perde non si perde nessuna carta.
import { ApiError, GENERIC, translate } from './api'
import { authedRequest, getSession } from './auth'
import { toBackendFaction, toOwnedCard, type CardResponse } from './cards'
import type { FactionId, OwnedCard } from '../types/deck'

export type PackType = {
  code: string
  price: number
  size: number
  slots: Record<string, number>
  supportSlots: number
  featured: boolean
  set: { code: string; name: string }
}

type PackOpenResponse = { packId: string; packType: string; snapshotId: string; price: number; creditsAfter: number; cards: CardResponse[] }

/** Una busta comprata: isNew[i] dice se la carta i era nuova per la collezione (null = non si è potuto sapere). */
export type OpenedPack = { id: string; packType: string; faction: FactionId; cards: OwnedCard[]; isNew: (boolean | null)[] }

export const listPacks = () => authedRequest<PackType[]>('GET', '/packs')

/** Nome della busta dalle etichette del backend; per un tipo senza etichetta, il suo codice. */
export function packName(code: string) {
  const name = translate(`pack.type.${code}`)
  return name === GENERIC ? code : name
}

export const newPurchaseKey = () =>
  crypto.randomUUID?.() ??
  'xxxxxxxx-xxxx-4xxx-8xxx-xxxxxxxxxxxx'.replace(/x/g, () => Math.floor(Math.random() * 16).toString(16))

/** Errore dopo il quale non si sa se l'acquisto è avvenuto: si riprova con la stessa chiave, senza pagare due volte. */
export const isUncertain = (err: unknown) => err instanceof ApiError && (err.status === 0 || err.status >= 500)

/** Compra e apre una busta. `owned` = carte già possedute: viene aggiornato con quelle appena trovate. */
export async function buyPack(code: string, faction: FactionId, key: string, owned: Set<string> | null) {
  const r = await authedRequest<PackOpenResponse>(
    'POST',
    '/packs/open',
    { packType: code, faction: toBackendFaction(faction) },
    { 'Idempotency-Key': key },
  )
  const cards = r.cards.map((c) => toOwnedCard(c))
  const isNew = cards.map((c) => {
    if (!owned) return null
    const fresh = !owned.has(c.cardId)
    owned.add(c.cardId)
    return fresh
  })
  const pack: OpenedPack = { id: r.packId, packType: r.packType, faction, cards, isNew }
  addPending(pack)
  return { pack, creditsAfter: r.creditsAfter }
}

// ---------- Buste da rivelare ----------
const KEY = () => `quorma.buste.${getSession()?.username ?? 'ospite'}`
const memory = new Map<string, OpenedPack[]>() // ripiego se il browser non può salvare

export function pendingPacks(): OpenedPack[] {
  const key = KEY()
  const held = memory.get(key)
  if (held) return held
  try {
    const parsed = JSON.parse(localStorage.getItem(key) ?? '[]')
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function savePending(list: OpenedPack[]) {
  const key = KEY()
  memory.set(key, list)
  try {
    localStorage.setItem(key, JSON.stringify(list))
  } catch {
    /* browser pieno o senza salvataggio: restano in memoria finché la pagina è aperta */
  }
}

function addPending(pack: OpenedPack) {
  const list = pendingPacks()
  if (!list.some((p) => p.id === pack.id)) savePending([...list, pack]) // stessa chiave ripetuta = stessa busta
}

export const removePending = (id: string) => savePending(pendingPacks().filter((p) => p.id !== id))
