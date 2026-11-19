import type { Deck } from '../types/deck'

// Mazzi di prova, uguali all'immagine. NON sono usati: i mazzi veri arrivano dal backend (hooks/useDecks.ts).
// Per vederli senza backend: in hooks/useDecks.ts scrivi useState<Deck[]>(demoDecks)
export const demoDecks: Deck[] = [
  { id: '1', name: 'Mazzo 01', faction: 'umani', cards: 32, maxCards: 32, defense: true, outdated: false },
  { id: '2', name: 'Mazzo 02', faction: 'artificiali', cards: 32, maxCards: 32, defense: true, outdated: false },
  { id: '3', name: 'Mazzo 03', faction: 'umani', cards: 32, maxCards: 32, defense: false, outdated: false },
  { id: '4', name: 'Mazzo 04', faction: 'artificiali', cards: 32, maxCards: 32, defense: false, outdated: true },
]
