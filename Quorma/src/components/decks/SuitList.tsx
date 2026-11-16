import type { Suit } from '../../types/deck'

const ICONS = ['◈', '♆', '▲', '✧'] // icone provvisorie dei semi
const MAX_PER_SUIT = 13 // serve solo a calcolare la lunghezza delle barre

// "CREATURE PER SEME": una riga per ogni seme con barra e numero.
export default function SuitList({ suits }: { suits: Suit[] }) {
  return (
    <ul className="suit-list">
      {suits.map((suit, i) => (
        <li key={suit.name}>
          <span className="suit-icon">{ICONS[i % ICONS.length]}</span>
          <span className="suit-name">{suit.name}</span>
          <span className="suit-bar">
            <span style={{ width: `${(suit.value / MAX_PER_SUIT) * 100}%` }} />
          </span>
          <span className="suit-value">{suit.value}</span>
        </li>
      ))}
    </ul>
  )
}
