// Partite vere: serie al meglio delle 3 giocate dal motore sul backend.
// Una serie è sempre tra due giocatori: A è chi l'ha avviata, B l'altro. Ognuno mette un mazzo per fazione
// e le fazioni si scambiano a ogni partita. Il backend gioca tutta la serie in una volta: il log di ogni partita
// arriva solo in quella risposta; dopo, lo storico (GET /matches) ha solo gli esiti e il log si riottiene dal replay.
import { authedRequest } from './auth'
import { toBackendFaction } from './cards'
import { handName, handOrder } from '../data/profile'
import type { GameDetail, Result } from '../data/profile'
import type { FactionId } from '../types/deck'

export type Seat = 'A' | 'B'
type Outcome = 'A_WIN' | 'B_WIN' | 'DRAW'

// Solo le parti del log che usiamo (schema match-log del motore).
export type MatchLog = {
  turns: { evaluation: Record<Seat, { hand_type: string }> }[]
  result: { outcome: Outcome; turns_played: number; life: Record<Seat, number> }
}

export type GameResponse = {
  gameNumber: number
  matchId: string
  factionA: string
  factionB: string
  outcome: Outcome
  playedAt: string
  log?: MatchLog // solo nella risposta che ha giocato la serie
}

export type SeriesResponse = {
  id: string
  kind: 'CHALLENGE' | 'MATCHMAKING'
  code?: string // ch-… (sfida) o rnd-… (stanza); assente nel matchmaking
  playerA: { id: string; username: string }
  playerB: { id: string; username: string }
  mySeat: Seat
  status: string
  outcome: Outcome // visto da A
  finishedAt: string
  unread: boolean
  games: GameResponse[]
}

type ReplayResponse = { seriesId: string; gameNumber: number; matchId: string; outcome: Outcome; verified: boolean; log: MatchLog }

/** I mazzi da mettere in gioco: uno per fazione. */
export type DeckPick = Record<FactionId, string>

const decksBody = (decks: DeckPick) =>
  (Object.keys(decks) as FactionId[]).map((f) => ({ faction: toBackendFaction(f), deckId: decks[f] }))

// ---------- Chiamate ----------
/** Partita libera: il server sceglie un avversario a caso e gioca subito la serie contro i suoi mazzi di difesa. */
export const playMatchmaking = (startingFaction: FactionId, decks: DeckPick) =>
  authedRequest<SeriesResponse>('POST', '/matchmaking', { startingFaction: toBackendFaction(startingFaction), decks: decksBody(decks) })

/** Una pagina dello storico, dalla serie più recente. Il backend accetta al massimo 50 serie per pagina. */
export const listSeries = (page: number, size: number, unread = false) =>
  authedRequest<SeriesResponse[]>('GET', `/matches?page=${page}&size=${size}${unread ? '&unread=true' : ''}`)

/** Una serie dello storico, senza i log delle partite. */
export const getSeries = (seriesId: string) => authedRequest<SeriesResponse>('GET', `/matches/${seriesId}`)

/** Segna il risultato della serie come letto: non compare più tra i non letti. Si può ripetere. */
export const markSeriesSeen = (seriesId: string) => authedRequest<SeriesResponse>('POST', `/matches/${seriesId}/seen`)

/** Rigioca la partita n della serie sul motore e restituisce il log (verificato con l'hash salvato). */
export const getReplay = (seriesId: string, n: number) =>
  authedRequest<ReplayResponse>('GET', `/matches/${seriesId}/games/${n}/replay`)

// ---------- Dal punto di vista del giocatore ----------
export const otherSeat = (s: Seat): Seat => (s === 'A' ? 'B' : 'A')

/** Vittoria, sconfitta o pareggio per chi siede in `seat`. */
export function resultFor(outcome: Outcome, seat: Seat): Result {
  if (outcome === 'DRAW') return 'pareggio'
  return outcome === `${seat}_WIN` ? 'vittoria' : 'sconfitta'
}

/** Esito della serie per me e il punteggio delle partite (es. vittoria 2–1). */
export function seriesScore(s: SeriesResponse) {
  const results = s.games.map((g) => resultFor(g.outcome, s.mySeat))
  return {
    result: resultFor(s.outcome, s.mySeat),
    won: results.filter((r) => r === 'vittoria').length,
    lost: results.filter((r) => r === 'sconfitta').length,
    opponent: (s.mySeat === 'A' ? s.playerB : s.playerA).username,
  }
}

/** Dal log di una partita: turni, vite finali e mani, viste da chi siede in `me`. */
export function detailFromLog(log: MatchLog, me: Seat): GameDetail {
  const them = otherSeat(me)
  const turnHands = log.turns.map((t) => [handName(t.evaluation[me]?.hand_type), handName(t.evaluation[them]?.hand_type)] as [string, string])
  const best = turnHands.map(([mine]) => mine).sort((a, b) => handOrder.indexOf(b) - handOrder.indexOf(a))[0] ?? handName('NONE')
  return {
    turns: log.result.turns_played,
    lifeMine: Math.max(0, log.result.life[me]),
    lifeOpp: Math.max(0, log.result.life[them]),
    best,
    turnHands,
  }
}
