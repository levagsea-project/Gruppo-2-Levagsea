import type { FactionId } from '../types/deck'

// Tipi e costanti del profilo: partite giocate, statistiche e impostazioni.
export type Result = 'vittoria' | 'sconfitta' | 'pareggio'
export const resultLabel: Record<Result, string> = { vittoria: 'Vittoria', sconfitta: 'Sconfitta', pareggio: 'Pareggio' }

// Le mani di poker, dalla più debole alla più forte: stesso ordine del motore (HandType, letto al contrario).
// "Nessuna mano" = nessuna creatura in campo. I nomi sono quelli del vocabolario del motore (HandTypeCondition).
const HANDS: [engine: string, name: string][] = [
  ['NONE', 'Nessuna mano'],
  ['HIGH_CARD', 'Carta alta'],
  ['PAIR', 'Coppia'],
  ['TWO_PAIR', 'Doppia coppia'],
  ['FOUR_OF_A_SUIT', 'Quattro dello stesso seme'],
  ['THREE_OF_A_KIND', 'Tris'],
  ['STRAIGHT', 'Scala'],
  ['FLUSH', 'Colore'],
  ['FULL_HOUSE', 'Full'],
  ['FOUR_OF_A_KIND', 'Poker'],
  ['STRAIGHT_FLUSH', 'Scala colore'],
  ['ROYAL_FLUSH', 'Scala reale'],
  ['FIVE_OF_A_KIND', 'Five of a kind'],
]
export const handOrder = HANDS.map(([, name]) => name)
const HAND_NAMES = new Map(HANDS)

/** Nome italiano di una mano del motore (es. TWO_PAIR → "Doppia coppia"). */
export const handName = (engine: string | undefined) => HAND_NAMES.get(engine ?? 'NONE') ?? engine ?? 'Nessuna mano'

// ---------- Storico (GET /matches, lib/matches.ts) ----------
// Si gioca a serie al meglio delle 3: lo storico è un elenco di serie, ognuna con le sue partite.
// Tutto è già visto da me: risultato, fazioni e avversario sono girati secondo il mio posto (mySeat).

// Quello che si sa di una partita solo dal suo log: arriva con la serie appena giocata o dal replay.
export type GameDetail = {
  turns: number
  lifeMine: number // vita finale (0 se è scesa sotto zero)
  lifeOpp: number
  best: string // la mia mano migliore della partita
  turnHands: [string, string][] // per ogni turno: [mia mano, mano avversaria]
}

// Una partita della serie.
export type Game = {
  n: number // 1, 2 o 3
  matchId: string
  result: Result
  myFaction: FactionId
  oppFaction: FactionId
  playedAt: number // millisecondi
  deck: string | null // nome del mio mazzo: il backend non lo dà, lo sappiamo solo per le serie avviate da questo browser
  detail: GameDetail | null // null = log non ancora letto (si chiede al replay)
}

// Una serie: chi, come, com'è finita.
export type Series = {
  id: string
  seat: 'A' | 'B' // il mio posto nella serie (A = l'ho avviata io): serve per leggere il log del replay
  mode: string // "Partita libera", "Sfida" o "Stanza"
  code: string | null // ch-XXXX-XXXX (sfida) o rnd-XXXX-XXXX (stanza); assente nel matchmaking
  opponent: string
  result: Result
  won: number // partite vinte da me
  lost: number
  finishedAt: number // millisecondi
  unread: boolean // risultato non ancora letto (sfide giocate mentre ero offline)
  games: Game[]
}

// Statistiche calcolate dallo storico (lib/stats.ts).
export type Overview = {
  total: number // serie giocate
  wins: number // serie vinte
  losses: number
  draws: number
  detailed: number // partite di cui conosciamo il log: le statistiche sulle mani si basano su queste
  hands: { name: string; percent: number }[] // le 5 mani più frequenti
  bestHand: { name: string; times: number } | null
  faction: { id: FactionId; percent: number; games: number } | null // fazione con cui vinco di più (% partite vinte)
}

// Impostazioni
export type Settings = {
  music: number
  effects: number
  muteAll: boolean
  speed: 'Lenta' | 'Normale' | 'Veloce'
  showHands: boolean
  confirmPlay: boolean
  reduceMotion: boolean
  highContrast: boolean
}

export const defaultSettings: Settings = {
  music: 70, effects: 80, muteAll: false, speed: 'Normale',
  showHands: true, confirmPlay: false, reduceMotion: false, highContrast: false,
}
export const speeds: Settings['speed'][] = ['Lenta', 'Normale', 'Veloce']
