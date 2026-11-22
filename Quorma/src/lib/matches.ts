import { getSession } from './auth'
import { toFaction } from './cards'
import { detailFromLog, getReplay, listSeries, resultFor, seriesScore, type SeriesResponse } from './series'
import type { Game, GameDetail, Series } from '../data/profile'
import type { FactionId } from '../types/deck'

// Lo storico delle partite. La fonte è il backend (GET /matches): serie, partite ed esiti.
// Nel browser (localStorage, uno per account) teniamo solo quello che il backend non restituisce nello storico:
//   - il dettaglio di ogni partita (turni, mani, vite), letto dal log. Il log arriva con la serie appena giocata
//     (rememberSeries) o dal replay (loadGameDetail); una partita è deterministica, quindi non cambia mai;
//   - i nomi dei mazzi usati, solo per le serie avviate da questo browser.
// Se il browser perde questi dati non si perde nulla: il dettaglio si richiede al replay, i nomi dei mazzi mancano.

const PAGE_SIZE = 50 // il massimo del backend
const MAX_PAGES = 4 // fino a 200 serie: oltre, il profilo mostra le più recenti
const MAX_DETAILS = 400
const MAX_DECK_NAMES = 200

const user = () => getSession()?.username ?? 'ospite'
const DETAILS_KEY = () => `quorma.partite.${user()}` // matchId → GameDetail
const DECKS_KEY = () => `quorma.mazzi-serie.${user()}` // seriesId → nome del mazzo per fazione

function readMap<T>(key: string): Record<string, T> {
  try {
    const raw = localStorage.getItem(key)
    const map = raw ? JSON.parse(raw) : {}
    return map && typeof map === 'object' && !Array.isArray(map) ? map : {}
  } catch {
    return {}
  }
}

// Aggiunge voci in fondo e tiene solo le ultime `max` (l'ordine di inserimento è l'età).
function writeMap<T>(key: string, add: Record<string, T>, max: number) {
  const map = { ...readMap<T>(key) }
  for (const [k, v] of Object.entries(add)) {
    delete map[k]
    map[k] = v
  }
  const keys = Object.keys(map)
  for (const k of keys.slice(0, Math.max(0, keys.length - max))) delete map[k]
  try {
    localStorage.setItem(key, JSON.stringify(map))
  } catch {
    /* browser pieno o senza salvataggio: il dettaglio si richiederà al replay */
  }
}

/** Dopo aver giocato una serie: ne salva il dettaglio delle partite (dal log) e i nomi dei mazzi usati. */
export function rememberSeries(s: SeriesResponse, deckNames: Record<FactionId, string>) {
  const details: Record<string, GameDetail> = {}
  for (const g of s.games) if (g.log) details[g.matchId] = detailFromLog(g.log, s.mySeat)
  writeMap(DETAILS_KEY(), details, MAX_DETAILS)
  writeMap(DECKS_KEY(), { [s.id]: deckNames }, MAX_DECK_NAMES)
}

/** Lo storico grezzo dal backend, dalla serie più recente. complete = false se ci sono più serie di quelle lette. */
export async function loadHistory(): Promise<{ list: SeriesResponse[]; complete: boolean }> {
  // L'archivio delle prime versioni (partite simulate o salvate solo qui) non serve più.
  try {
    localStorage.removeItem(`quorma.matches.${user()}`)
  } catch {
    /* niente da togliere */
  }
  const list: SeriesResponse[] = []
  for (let page = 0; page < MAX_PAGES; page++) {
    const batch = await listSeries(page, PAGE_SIZE)
    list.push(...batch)
    if (batch.length < PAGE_SIZE) return { list, complete: true }
  }
  return { list, complete: false }
}

const MODES = { MATCHMAKING: 'Partita libera', ROOM: 'Stanza', CHALLENGE: 'Sfida' } as const

/** Le serie del backend viste da me, con i dettagli e i nomi dei mazzi che il browser ricorda. */
export function toSeriesList(list: SeriesResponse[]): Series[] {
  const details = readMap<GameDetail>(DETAILS_KEY())
  const decks = readMap<Record<FactionId, string>>(DECKS_KEY())
  return list.map((s) => toSeries(s, details, decks[s.id]))
}

function toSeries(s: SeriesResponse, details: Record<string, GameDetail>, decks: Record<FactionId, string> | undefined): Series {
  const me = s.mySeat
  const score = seriesScore(s)
  const games: Game[] = [...s.games]
    .sort((a, b) => a.gameNumber - b.gameNumber)
    .map((g) => {
      const myFaction = toFaction(me === 'A' ? g.factionA : g.factionB)
      return {
        n: g.gameNumber,
        matchId: g.matchId,
        result: resultFor(g.outcome, me),
        myFaction,
        oppFaction: toFaction(me === 'A' ? g.factionB : g.factionA),
        playedAt: Date.parse(g.playedAt),
        deck: decks?.[myFaction] ?? null,
        detail: details[g.matchId] ?? (g.log ? detailFromLog(g.log, me) : null),
      }
    })
  return {
    id: s.id,
    seat: me,
    mode: s.kind === 'MATCHMAKING' ? MODES.MATCHMAKING : s.code?.startsWith('rnd-') ? MODES.ROOM : MODES.CHALLENGE,
    code: s.code ?? null,
    opponent: score.opponent,
    result: score.result,
    won: score.won,
    lost: score.lost,
    finishedAt: Date.parse(s.finishedAt),
    unread: s.unread,
    games,
  }
}

/** Il dettaglio di una partita dal replay (il motore la rigioca): lo salva e lo restituisce. */
export async function loadGameDetail(series: Series, game: Game): Promise<GameDetail> {
  const replay = await getReplay(series.id, game.n)
  const detail = detailFromLog(replay.log, series.seat)
  writeMap(DETAILS_KEY(), { [game.matchId]: detail }, MAX_DETAILS)
  return detail
}

const day = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()

/** "Oggi, 18:40" · "Ieri, 22:12" · "12 nov, 20:47" */
export function formatWhen(timestamp: number): string {
  const d = new Date(timestamp)
  const time = d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
  const daysAgo = Math.round((day(new Date()) - day(d)) / 86_400_000)
  if (daysAgo === 0) return `Oggi, ${time}`
  if (daysAgo === 1) return `Ieri, ${time}`
  return `${d.getDate()} ${d.toLocaleDateString('it-IT', { month: 'short' }).replace('.', '')}, ${time}`
}
