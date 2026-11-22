import type { GameCard } from '../../types/deck'

const fmt = (n: number | null) => (n === null ? '–' : n.toLocaleString('it-IT'))

// ATK e DEF sotto una carta piccola delle griglie: sotto i 140px nella cornice non si leggerebbero (card-frame.css).
// Per i lettori di schermo i valori stanno già nell'etichetta del bottone della carta.
export default function CardStats({ card }: { card: GameCard }) {
  if (card.kind !== 'CREATURE') return null
  return (
    <span className="q-cf-valori" aria-hidden="true">
      <span><small>ATK</small>{fmt(card.atk)}</span>
      <span><small>DEF</small>{fmt(card.def)}</span>
    </span>
  )
}
