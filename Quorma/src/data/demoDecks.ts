import type { Deck } from '../types/deck'

// Mazzi di prova, uguali all'immagine. NON sono usati: l'app parte con 0 mazzi.
// Per vederli: in hooks/useDecks.ts scrivi useState<Deck[]>(demoDecks)
const suits = [
  { name: 'Vincolo', value: 8 },
  { name: 'Frattura', value: 6 },
  { name: 'Spira', value: 7 },
  { name: 'Vestigio', value: 5 },
]

export const demoDecks: Deck[] = [
  { id: '1', name: 'Mazzo 01', faction: 'umani', cards: 32, maxCards: 32, creatures: 24, spells: 8, suits },
  { id: '2', name: 'Mazzo 02', faction: 'artificiali', cards: 32, maxCards: 32, creatures: 26, spells: 6, suits },
  { id: '3', name: 'Mazzo 03', faction: 'umani', cards: 32, maxCards: 32, creatures: 22, spells: 10, suits },
  { id: '4', name: 'Mazzo 04', faction: 'artificiali', cards: 26, maxCards: 32, creatures: 20, spells: 6, suits },
]
