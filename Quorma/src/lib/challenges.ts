// Sfide per nome (/challenges) e stanze (/rooms). Accettare una sfida o entrare in una stanza gioca subito la serie.
import { ApiError } from './api'
import { authedRequest } from './auth'
import { toBackendFaction } from './cards'
import type { DeckPick, SeriesResponse } from './series'
import type { FactionId } from '../types/deck'

type UserRef = { id: string; username: string }

export type Challenge = {
  id: string
  code: string
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED' | 'EXPIRED'
  challenger: UserRef
  opponent: UserRef
  challengerFaction: string
  createdAt: string
  expiresAt: string
  resolvedAt?: string | null
  seriesId?: string | null
}

export type Room = {
  id: string
  code: string
  creator: UserRef
  creatorFaction: string
  status: 'OPEN' | 'MATCHED' | 'EXPIRED' | 'CANCELLED'
  createdAt: string
  expiresAt: string
  seriesId?: string
}

const decksBody = (decks: DeckPick) =>
  (Object.keys(decks) as FactionId[]).map((f) => ({ faction: toBackendFaction(f), deckId: decks[f] }))

export const listChallenges = () => authedRequest<Challenge[]>('GET', '/challenges')
export const createChallenge = (opponentUsername: string, startingFaction: FactionId, decks: DeckPick) =>
  authedRequest<Challenge>('POST', '/challenges', { opponentUsername, startingFaction: toBackendFaction(startingFaction), decks: decksBody(decks) })
export const acceptChallenge = (id: string, decks: DeckPick) =>
  authedRequest<SeriesResponse>('POST', `/challenges/${id}/accept`, { decks: decksBody(decks) })
export const declineChallenge = (id: string) => authedRequest<Challenge>('POST', `/challenges/${id}/decline`)
export const cancelChallenge = (id: string) => authedRequest<Challenge>('POST', `/challenges/${id}/cancel`)

export const listRooms = () => authedRequest<Room[]>('GET', '/rooms')
export const openRoom = (startingFaction: FactionId, decks: DeckPick) =>
  authedRequest<Room>('POST', '/rooms', { startingFaction: toBackendFaction(startingFaction), decks: decksBody(decks) })
export const joinRoom = (code: string, decks: DeckPick) =>
  authedRequest<SeriesResponse>('POST', `/rooms/${code}/join`, { decks: decksBody(decks) })
export const cancelRoom = (code: string) => authedRequest<Room>('POST', `/rooms/${code}/cancel`)

/** L'ultima stanza che ho aperto; null se non ne ho mai aperta una (404). */
export async function getMyRoom(): Promise<Room | null> {
  try {
    return await authedRequest<Room>('GET', '/rooms/mine')
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) return null
    throw err
  }
}

/** "47 h" · "12 min" · "meno di un minuto" */
export function timeLeft(iso: string, now = Date.now()) {
  const minutes = Math.floor((Date.parse(iso) - now) / 60_000)
  if (minutes < 1) return 'meno di un minuto'
  return minutes >= 60 ? `${Math.floor(minutes / 60)} h` : `${minutes} min`
}
