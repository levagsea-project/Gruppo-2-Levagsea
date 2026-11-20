import type { Match } from '../data/profile'
import { handOrder } from '../data/profile'
import type { FactionId } from '../types/deck'

// PARTITA SIMULATA. Il gioco vero non esiste ancora: questa funzione "gioca" una partita a caso
// e restituisce il risultato, così profilo e cronologia si possono provare.
// Quando ci sarà il gioco vero, a fine partita basta creare un Match e chiamare recordMatch().

const START_LIFE = 4000
const MAX_TURNS = 50

// Quanto è probabile ogni mano (stesso ordine di handOrder: le mani forti sono rare).
const WEIGHTS = [30, 30, 14, 9, 5, 5, 4, 2, 1]

const pickHand = () => {
  let roll = Math.random() * WEIGHTS.reduce((a, b) => a + b, 0)
  for (let i = 0; i < WEIGHTS.length; i++) {
    roll -= WEIGHTS[i]
    if (roll < 0) return i
  }
  return 0
}

const hex = (n: number) => Array.from({ length: n }, () => Math.floor(Math.random() * 16).toString(16)).join('').toUpperCase()

export function simulateMatch(deck: { name: string; faction: FactionId }, mode: string): Match {
  let mine = START_LIFE
  let theirs = START_LIFE
  const turnHands: [string, string][] = []
  let best = 0

  // A ogni turno vince la mano più forte e fa danno all'altro (più è grande la differenza, più danno).
  while (mine > 0 && theirs > 0 && turnHands.length < MAX_TURNS) {
    const a = pickHand()
    const b = pickHand()
    turnHands.push([handOrder[a], handOrder[b]])
    best = Math.max(best, a)
    if (a > b) theirs -= 150 * (a - b) + 100
    else if (b > a) mine -= 150 * (b - a) + 100
  }
  mine = Math.max(0, mine)
  theirs = Math.max(0, theirs)

  return {
    id: `${Date.now().toString(36)}${hex(4)}`,
    playedAt: Date.now(),
    result: mine > theirs ? 'vittoria' : mine < theirs ? 'sconfitta' : 'pareggio',
    opponent: 'Computer',
    mode,
    deck: deck.name,
    myFaction: deck.faction,
    oppFaction: Math.random() < 0.5 ? 'umani' : 'artificiali',
    turns: turnHands.length,
    lifeMine: mine,
    lifeOpp: theirs,
    best: handOrder[best],
    code: `QRM-${hex(4)}-${hex(4)}`,
    turnHands,
    simulated: true,
  }
}
