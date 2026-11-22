import type { Suit } from '../../types/deck'

const MAX_PER_SUIT = 13 // serve solo a calcolare la lunghezza delle barre (13 ranghi per seme)

// Classe dell'icona del seme dal nome ("Vincolo" → "vincolo"): le icone sono in public/images/ui/semi.
const seedClass = (name: string) => `deck-seme deck-seme--${name.toLowerCase()}`

// "Creature per seme": una riga per seme con icona, barra e numero.
export default function SuitList({ suits }: { suits: Suit[] }) {
  return (
    <ul className="suit-list">
      {suits.map((suit) => (
        <li key={suit.name}>
          <span className={seedClass(suit.name)} aria-hidden="true" />
          <span className="suit-name">{suit.name}</span>
          <span className="suit-bar" aria-hidden="true">
            <span style={{ width: `${(suit.value / MAX_PER_SUIT) * 100}%` }} />
          </span>
          <span className="suit-value">{suit.value}</span>
        </li>
      ))}
    </ul>
  )
}
