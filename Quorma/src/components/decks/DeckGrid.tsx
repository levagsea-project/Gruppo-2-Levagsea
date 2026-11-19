import type { Deck } from '../../types/deck'
import NewDeckCard from './NewDeckCard'
import DeckCard from './DeckCard'
import '../../styles/play.css' // .pick e .q-mazzo (carta-mazzo con gli strati), condivisi con la pagina Gioca

type Props = {
  decks: Deck[]
  selectedId?: string
  onSelect: (id: string) => void
  onNewDeck: () => void
}

// La griglia: prima "Nuovo mazzo", poi tutti i mazzi. Le colonne si adattano alla larghezza (styles/decks.css).
export default function DeckGrid({ decks, selectedId, onSelect, onNewDeck }: Props) {
  return (
    <div className="deck-grid">
      <NewDeckCard onClick={onNewDeck} />
      {decks.map((deck) => (
        <DeckCard key={deck.id} deck={deck} selected={deck.id === selectedId} onSelect={() => onSelect(deck.id)} />
      ))}
    </div>
  )
}
