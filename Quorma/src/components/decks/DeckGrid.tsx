import type { Deck } from '../../types/deck'
import NewDeckCard from './NewDeckCard'
import DeckCard from './DeckCard'

type Props = {
  decks: Deck[]
  selectedId?: string
  onSelect: (id: string) => void
  onNewDeck: () => void
}

// La griglia: prima "Nuovo mazzo", poi tutti i mazzi creati.
export default function DeckGrid({ decks, selectedId, onSelect, onNewDeck }: Props) {
  return (
    <div className="deck-grid">
      <NewDeckCard onClick={onNewDeck} />
      {decks.map((deck) => (
        <DeckCard
          key={deck.id}
          deck={deck}
          selected={deck.id === selectedId}
          onSelect={() => onSelect(deck.id)}
        />
      ))}
    </div>
  )
}
