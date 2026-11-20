// Il profilo. Il backend per ora dà solo il nome: statistiche e partite NON esistono ancora,
// quindi in un accesso vero la pagina mostra "in arrivo" (PRODUCT.md, principio 4).
// I dati "demo" qui sotto si vedono SOLO con l'accesso demo (VITE_AUTH_DEMO=true), per provare il layout.

export type Result = 'vittoria' | 'sconfitta' | 'pareggio'
export const resultLabel: Record<Result, string> = { vittoria: 'Vittoria', sconfitta: 'Sconfitta', pareggio: 'Pareggio' }

// Panoramica
export const handNames = ['Coppia', 'Doppia coppia', 'Tris', 'Carta alta', 'Quattro dello stesso seme']
export const statLabels = ['Partite', 'Vittorie', 'Sconfitte', 'Pareggi']

export type Overview = {
  stats: { value: number; extra?: string }[] // nello stesso ordine di statLabels
  hands: { name: string; percent: number }[]
  bestHand: { name: string; detail: string }
  faction: { name: string; detail: string }
}

const demoStats = [
  { value: 148 },
  { value: 86, extra: '58%' },
  { value: 57 },
  { value: 5 },
]

const demoHands = [
  { name: 'Coppia', percent: 38 },
  { name: 'Doppia coppia', percent: 21 },
  { name: 'Tris', percent: 14 },
  { name: 'Carta alta', percent: 11 },
  { name: 'Quattro dello stesso seme', percent: 7 },
]

export const demoOverview: Overview = {
  stats: demoStats,
  hands: demoHands,
  bestHand: { name: 'Scala colore', detail: 'Ottenuta 2 volte' },
  faction: { name: 'Potenze Artificiali', detail: '62% delle partite · Mazzo 02' },
}

// Storico partite
export type Match = {
  id: string
  result: Result
  opponent: string
  mode: string
  deck: string // es. "Mazzo 02"
  myFaction: string
  oppFaction: string
  turns: number
  when: string
  lifeMine: string
  lifeOpp: string
  best: string // mano migliore
  code: string
}

export const demoMatches: Match[] = [
  { id: 'm1', result: 'vittoria', opponent: 'Giocatore 27', mode: 'Classificata', deck: 'Mazzo 02', myFaction: 'Potenze Artificiali', oppFaction: 'Resistenza Umana', turns: 14, when: 'Oggi, 18:40', lifeMine: '3.420', lifeOpp: '0', best: 'Full', code: 'QRM-7F3A-91C2' },
  { id: 'm2', result: 'sconfitta', opponent: 'Giocatore 08', mode: 'Classificata', deck: 'Mazzo 02', myFaction: 'Potenze Artificiali', oppFaction: 'Potenze Artificiali', turns: 21, when: 'Oggi, 18:05', lifeMine: '0', lifeOpp: '1.980', best: 'Tris', code: 'QRM-2B8D-44E1' },
  { id: 'm3', result: 'vittoria', opponent: 'Computer', mode: 'Allenamento', deck: 'Mazzo 01', myFaction: 'Resistenza Umana', oppFaction: 'Potenze Artificiali', turns: 9, when: 'Ieri, 22:12', lifeMine: '4.100', lifeOpp: '0', best: 'Colore', code: 'QRM-9C01-A3F7' },
  { id: 'm4', result: 'pareggio', opponent: 'Giocatore 54', mode: 'Partita libera', deck: 'Mazzo 03', myFaction: 'Resistenza Umana', oppFaction: 'Resistenza Umana', turns: 50, when: 'Ieri, 21:30', lifeMine: '1.200', lifeOpp: '1.200', best: 'Doppia coppia', code: 'QRM-55D2-0B6E' },
  { id: 'm5', result: 'vittoria', opponent: 'Giocatore 13', mode: 'Classificata', deck: 'Mazzo 02', myFaction: 'Potenze Artificiali', oppFaction: 'Resistenza Umana', turns: 17, when: '12 nov, 20:47', lifeMine: '2.760', lifeOpp: '0', best: 'Scala', code: 'QRM-A417-6D9C' },
  { id: 'm6', result: 'sconfitta', opponent: 'Giocatore 31', mode: 'Partita libera', deck: 'Mazzo 01', myFaction: 'Resistenza Umana', oppFaction: 'Potenze Artificiali', turns: 12, when: '12 nov, 20:10', lifeMine: '0', lifeOpp: '2.300', best: 'Coppia', code: 'QRM-3E6F-B802' },
]

// Turni mostrati nel dettaglio di una partita (stessi per tutte, per ora).
export const turnRows = [
  ['Colore', 'Tris'],
  ['Doppia coppia', 'Full'],
  ['Quattro dello stesso seme', 'Quattro dello stesso seme'],
  ['Full', 'Doppia coppia'],
  ['Tris', 'Colore'],
  ['Coppia', 'Coppia'],
]

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
