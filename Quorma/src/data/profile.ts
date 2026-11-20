import type { FactionId } from '../types/deck'

// Tipi e costanti del profilo: partite giocate, statistiche e impostazioni.
export type Result = 'vittoria' | 'sconfitta' | 'pareggio'
export const resultLabel: Record<Result, string> = { vittoria: 'Vittoria', sconfitta: 'Sconfitta', pareggio: 'Pareggio' }

// Le mani di poker, dalla più debole alla più forte (l'ordine serve a decidere chi vince il turno).
export const handOrder = [
  'Carta alta', 'Coppia', 'Doppia coppia', 'Tris', 'Scala', 'Colore', 'Full', 'Quattro dello stesso seme', 'Scala colore',
]

// Una partita giocata. Si salva con recordMatch (lib/matches.ts) e da qui nascono statistiche e cronologia.
export type Match = {
  id: string
  playedAt: number // quando è finita (millisecondi, Date.now())
  result: Result
  opponent: string
  mode: string // es. "Classificata"
  deck: string // nome del mazzo usato, es. "Mazzo 02"
  myFaction: FactionId
  oppFaction: FactionId
  turns: number
  lifeMine: number // vita finale
  lifeOpp: number
  best: string // la tua mano migliore della partita
  code: string // codice per rivederla
  turnHands: [string, string][] // per ogni turno: [tua mano, mano avversaria]
  simulated: boolean // true = partita simulata (il gioco vero non esiste ancora)
}

// Statistiche calcolate dalle partite (lib/stats.ts).
export type Overview = {
  total: number
  wins: number
  losses: number
  draws: number
  hands: { name: string; percent: number }[] // le 5 mani più giocate
  bestHand: { name: string; times: number } | null
  faction: { id: FactionId; percent: number; deck: string } | null
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
