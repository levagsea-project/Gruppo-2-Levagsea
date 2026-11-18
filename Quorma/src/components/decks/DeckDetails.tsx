import type { Deck } from '../../types/deck'
import { factions } from '../../data/factions'
import Stat from '../ui/Stat'
import SuitList from './SuitList'
import DeckActions from './DeckActions'

type Props = { deck: Deck | null; onPlay: () => void; onDelete: (id: string) => void }

// Pannello a destra con i dettagli del mazzo scelto.
export default function DeckDetails({ deck, onPlay, onDelete }: Props) {
  if (!deck) {
    return (
      <aside className="deck-details">
        <p className="deck-empty">Nessun mazzo creato.<br />Premi "Nuovo mazzo" per iniziare.</p>
      </aside>
    )
  }

  const valid = deck.cards === deck.maxCards

  return (
    <aside className="deck-details">
      <p className="details-tag">MAZZO SCELTO</p>
      <h2 className="details-name">{deck.name.toUpperCase()}</h2>
      <p className="details-faction">{factions[deck.faction].name.toUpperCase()}</p>

      <div className="details-stats">
        <Stat value={deck.cards} label={`/ ${deck.maxCards} CARTE`} />
        <Stat value={deck.creatures} label="CREATURE" />
        <Stat value={deck.spells} label="MAGIE / EQUIP." />
      </div>

      <p className="details-tag">CREATURE PER SEME</p>
      <SuitList suits={deck.suits} />

      <p className="details-status">
        {valid ? 'Mazzo valido, pronto per giocare.' : 'Mazzo incompleto.'}
      </p>

      <DeckActions onPlay={onPlay} onDelete={() => onDelete(deck.id)} />
    </aside>
  )
}
