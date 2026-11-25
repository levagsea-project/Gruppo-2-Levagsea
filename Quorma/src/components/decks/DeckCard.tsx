import type { CSSProperties } from 'react'
import type { Deck } from '../../types/deck'
import { factions } from '../../data/factions'

type Props = { deck: Deck; selected: boolean; onSelect: () => void }

// Un mazzo come scatola di carte (design "Pagina-Mazzi"): fronte con l'arte della fazione, pila e dorso a ventaglio
// dietro, sotto nome e fazione. Scelto = bordo chiaro acceso.
export default function DeckCard({ deck, selected, onSelect }: Props) {
  const faction = factions[deck.faction]
  const incomplete = deck.cards < deck.maxCards
  const stato = deck.outdated ? 'Obsoleto' : incomplete ? `${deck.cards} / ${deck.maxCards} carte` : deck.defense ? 'Difesa' : ''
  const aria = [deck.name, faction.name, deck.outdated ? 'obsoleto' : '', incomplete ? `incompleto: ${deck.cards} carte su ${deck.maxCards}` : '', deck.defense ? 'mazzo di difesa' : '']
    .filter(Boolean)
    .join(', ')

  return (
    <button type="button" className="k-mazzo" aria-pressed={selected} aria-label={aria} onClick={onSelect}
      style={{ '--f': faction.tint, '--ft': faction.tintText } as CSSProperties}>
      <span className="k-scatola" aria-hidden="true">
        <span className="k-fronte">
          <span className="k-arte" style={{ backgroundImage: `url(${faction.cover})`, backgroundPosition: faction.coverFocus }} />
        </span>
      </span>
      <span className="k-targa" aria-hidden="true">
        <span className="k-nome" title={deck.name}>{deck.name}</span>
        <span className="k-fazione">{faction.name}</span>
        {stato && <span className={`k-stato ${deck.outdated || incomplete ? 'k-stato--off' : ''}`}>{stato}</span>}
      </span>
    </button>
  )
}
