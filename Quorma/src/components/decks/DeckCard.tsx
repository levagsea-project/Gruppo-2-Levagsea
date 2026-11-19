import type { CSSProperties } from 'react'
import type { Deck } from '../../types/deck'
import { factions } from '../../data/factions'

type Props = { deck: Deck; selected: boolean; onSelect: () => void }

// Una carta-mazzo del design system: due "strati" dietro (il mazzo), arco con l'arte della fazione,
// nome, fazione e stato. Scelta = filo petrolio acceso (q-acceso).
export default function DeckCard({ deck, selected, onSelect }: Props) {
  const faction = factions[deck.faction]
  const status = deck.outdated ? 'Obsoleto' : deck.defense ? 'Difesa' : 'Valido'

  return (
    <button type="button" className="pick deck-pick" aria-pressed={selected} onClick={onSelect}>
      <span className="q-mazzo">
        <span className="q-mazzo__strato" aria-hidden="true" />
        <span className="q-mazzo__strato" aria-hidden="true" />
        <span className={`q-tela q-tela--arco q-carta ${selected ? 'q-acceso' : ''}`}>
          <span className="q-tela__in">
            <span className="q-carta__arte" style={{ backgroundImage: `url(${faction.image})` } as CSSProperties} />
            <span className="q-divisorio" />
            <span className="q-carta__targa">
              <span className="q-carta__nome">{deck.name}</span>
              <span className="q-carta__sotto">{faction.name}</span>
              <span className={`deck-pick__stato ${deck.outdated ? 'deck-pick__stato--off' : ''}`}>
                {deck.cards} / {deck.maxCards} · {status}
              </span>
            </span>
          </span>
          <span className="q-borchia q-borchia--n" aria-hidden="true" />
        </span>
      </span>
    </button>
  )
}
