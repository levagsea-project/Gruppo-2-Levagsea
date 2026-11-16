import type { Deck } from '../../types/deck'
import { factions } from '../../data/factions'

type Props = { deck: Deck; selected: boolean; onSelect: () => void }

// Una carta-mazzo: immagine della fazione + nome + stato.
export default function DeckCard({ deck, selected, onSelect }: Props) {
  const faction = factions[deck.faction]
  const valid = deck.cards === deck.maxCards

  return (
    <button className={`deck-card ${selected ? 'is-selected' : ''}`} onClick={onSelect}>
      <span className="deck-diamond" />
      <img className="deck-img" src={faction.image} alt="" />
      <span className="deck-info">
        <span className="deck-name">{deck.name.toUpperCase()}</span>
        <span className="deck-faction">{faction.name.toUpperCase()}</span>
        <span className="deck-status">
          {deck.cards} / {deck.maxCards} · {valid ? 'Valido' : 'Incompleto'}
        </span>
      </span>
    </button>
  )
}
