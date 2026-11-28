// Amici e scambi (design: Pagina-Scambi e Pagina-Scambi-Proposta).
//
// Gli endpoint sono quelli del documento "Quorma — API mancanti per il backend" (24 nov 2026): il backend non li ha
// ancora. Finché mancano, il server risponde 404 senza `code` (rotta inesistente): lo riconosce `isMissingApi`.
// In sviluppo (npm run dev) la pagina passa allora ai dati di prova di data/demoTrades.ts, così si può provare
// tutto il giro; nella build di produzione mostra che gli scambi non sono ancora attivi.
import { ApiError, GENERIC } from './api'
import { authedRequest } from './auth'
import { toOwnedCard, type CardResponse } from './cards'
import type { OwnedCard } from '../types/deck'

// ---------- Forma dei dati ----------
export type Player = { username: string; displayName: string; avatar: string | null }
export type Friend = Player & { since: string }
export type FriendRequest = Player & { id: string; createdAt: string }
export type FriendRequests = { received: FriendRequest[]; sent: FriendRequest[] }

export type TradeStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED'
export type Trade = {
  id: string
  status: TradeStatus
  myRole: 'PROPOSER' | 'RECIPIENT'
  proposer: Player
  recipient: Player
  offer: OwnedCard[] // carte del proponente
  request: OwnedCard[] // carte del destinatario
  createdAt: string
  resolvedAt: string | null
}

/** Regole del documento: almeno una carta, al massimo 10 per parte. */
export const TRADE_MAX_PER_SIDE = 10

// ---------- Dal punto di vista di chi guarda ----------
export const otherPlayer = (t: Trade) => (t.myRole === 'PROPOSER' ? t.recipient : t.proposer)
export const cardsIGive = (t: Trade) => (t.myRole === 'PROPOSER' ? t.offer : t.request)
export const cardsIGet = (t: Trade) => (t.myRole === 'PROPOSER' ? t.request : t.offer)

/** Le tre schede della pagina: proposte da rispondere, proposte mie in attesa, concluse. */
export type TradeTab = 'arrivo' | 'inviate' | 'concluse'
export const tabOf = (t: Trade): TradeTab => (t.status !== 'PENDING' ? 'concluse' : t.myRole === 'RECIPIENT' ? 'arrivo' : 'inviate')

// ---------- Errori ----------
// Il backend non ha ancora le frasi di questi codici in data/labels/it.json: finché mancano le teniamo qui.
// Se un giorno le aggiunge, vince la sua frase (ApiError.message).
const MESSAGES: Record<string, string> = {
  'user.not_found': 'Nessun giocatore con questo nome.',
  'friend.not_found': 'Questo giocatore non è tra i tuoi amici.',
  'friend.self': 'Non puoi aggiungere te stesso.',
  'friend.already_friends': 'Siete già amici.',
  'friend.request_pending': 'C’è già una richiesta in attesa con questo giocatore.',
  'friend.blocked': 'Non puoi inviare una richiesta a questo giocatore.',
  'friend.limit': 'Hai raggiunto il limite di amici o di richieste in attesa.',
  'friend.request_not_found': 'Questa richiesta non esiste più.',
  'friend.request_not_pending': 'Questa richiesta è già stata gestita.',
  'trade.not_friends': 'Puoi scambiare carte solo con i tuoi amici.',
  'trade.empty': 'Scegli almeno una carta.',
  'trade.too_many_cards': `Al massimo ${TRADE_MAX_PER_SIDE} carte per parte.`,
  'trade.card_not_owned': 'Una delle carte scelte non appartiene più a chi la offre.',
  'trade.card_in_defense_deck': 'Le carte del mazzo di difesa non si possono scambiare.',
  'trade.limit': 'Hai già troppe proposte in attesa: aspetta una risposta o annullane una.',
  'trade.not_found': 'Questo scambio non esiste più.',
  'trade.not_pending': 'Questo scambio è già concluso.',
  'trade.card_unavailable': 'Una carta non è più disponibile: lo scambio è stato annullato.',
}

/** Frase per il giocatore da un errore di amici o scambi. */
export function tradeError(err: unknown): string {
  if (!(err instanceof ApiError)) return GENERIC
  if (err.code && MESSAGES[err.code] && err.message === GENERIC) return MESSAGES[err.code]
  return err.message
}

/** Il server non ha ancora questo endpoint: 404 (o 405) senza un codice nostro. */
export const isMissingApi = (err: unknown) => err instanceof ApiError && (err.status === 404 || err.status === 405) && !err.code

// ---------- Risposte del backend ----------
type TradeCardResponse = CardResponse & { instanceId?: string }
type TradeResponse = Omit<Trade, 'offer' | 'request'> & { offer: TradeCardResponse[]; request: TradeCardResponse[] }
type Paged<T> = T[] | { content: T[] }

// Nel documento le carte dello scambio hanno "instanceId" più i campi di CollectionCardResponse (che lo chiama
// cardInstanceId): accettiamo entrambi.
const toCard = (c: TradeCardResponse) => toOwnedCard({ ...c, cardInstanceId: c.cardInstanceId ?? c.instanceId ?? '' })
const toTrade = (t: TradeResponse): Trade => ({ ...t, offer: t.offer.map(toCard), request: t.request.map(toCard) })
const items = <T,>(p: Paged<T>) => (Array.isArray(p) ? p : p.content)
const user = (username: string) => encodeURIComponent(username)

// ---------- Amici ----------
export const listFriends = () => authedRequest<Friend[]>('GET', '/friends')
export const listFriendRequests = () => authedRequest<FriendRequests>('GET', '/friends/requests')
/** 201 con la richiesta; se l'altro mi aveva già invitato diventa subito amicizia (200, {friend}). */
export const sendFriendRequest = (username: string) =>
  authedRequest<FriendRequest | { friend: Friend }>('POST', '/friends/requests', { username: username.trim() })
export const acceptFriendRequest = (id: string) => authedRequest<Friend>('POST', `/friends/requests/${id}/accept`)
export const declineFriendRequest = (id: string) => authedRequest<null>('POST', `/friends/requests/${id}/decline`)
export const cancelFriendRequest = (id: string) => authedRequest<null>('POST', `/friends/requests/${id}/cancel`)

/** Le carte scambiabili di un amico (stessa forma di GET /collection). */
export async function getFriendCollection(username: string): Promise<OwnedCard[]> {
  const r = await authedRequest<{ cards: CardResponse[] }>('GET', `/friends/${user(username)}/collection`)
  return r.cards.map((c) => toOwnedCard(c))
}

// ---------- Scambi ----------
/** I miei scambi, più recenti prima (il backend li pagina: chiediamo una pagina larga). */
export const listTrades = async () => items(await authedRequest<Paged<TradeResponse>>('GET', '/trades?page=0&size=100')).map(toTrade)

export const createTrade = async (recipientUsername: string, offer: string[], request: string[]) =>
  toTrade(await authedRequest<TradeResponse>('POST', '/trades', { recipientUsername, offer, request }))

export const acceptTrade = async (id: string) => toTrade(await authedRequest<TradeResponse>('POST', `/trades/${id}/accept`))
export const declineTrade = async (id: string) => toTrade(await authedRequest<TradeResponse>('POST', `/trades/${id}/decline`))
export const cancelTrade = async (id: string) => toTrade(await authedRequest<TradeResponse>('POST', `/trades/${id}/cancel`))

// ---------- Sorgente dei dati ----------
// La pagina parla con questo insieme di funzioni: il backend vero, oppure i dati di prova (data/demoTrades.ts).
export type TradesApi = {
  listFriends: () => Promise<Friend[]>
  listFriendRequests: () => Promise<FriendRequests>
  sendFriendRequest: (username: string) => Promise<FriendRequest | { friend: Friend }>
  acceptFriendRequest: (id: string) => Promise<Friend>
  declineFriendRequest: (id: string) => Promise<null>
  getFriendCollection: (username: string) => Promise<OwnedCard[]>
  listTrades: () => Promise<Trade[]>
  createTrade: (recipientUsername: string, offer: string[], request: string[]) => Promise<Trade>
  acceptTrade: (id: string) => Promise<Trade>
  declineTrade: (id: string) => Promise<Trade>
  cancelTrade: (id: string) => Promise<Trade>
}

export const backendTrades: TradesApi = {
  listFriends, listFriendRequests, sendFriendRequest, acceptFriendRequest, declineFriendRequest,
  getFriendCollection, listTrades, createTrade, acceptTrade, declineTrade, cancelTrade,
}

// ---------- Testi ----------
/** Iniziali per l'ogiva: due lettere dal nome visualizzato. */
export const initials = (p: Player) =>
  (p.displayName || p.username)
    .split(/[\s._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]!.toUpperCase())
    .join('')

const time = (d: Date) => d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })

/** "Oggi, 18:12", "Ieri, 21:05" oppure "12 nov". */
export function when(iso: string) {
  const d = new Date(iso)
  const today = new Date()
  const days = Math.round((new Date(today.toDateString()).getTime() - new Date(d.toDateString()).getTime()) / 86_400_000)
  if (days === 0) return `Oggi, ${time(d)}`
  if (days === 1) return `Ieri, ${time(d)}`
  return d.toLocaleDateString('it-IT', { day: 'numeric', month: 'short' })
}

export const cardsLabel = (n: number) => (n === 1 ? '1 carta' : `${n} carte`)
