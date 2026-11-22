import { cardBackFrame } from '../../data/cardArt'
import { SUIT_NAMES, creatureTier } from '../../lib/cards'
import type { GameCard } from '../../types/deck'

const KIND_NAME = { SPELL: 'Magia', EQUIPMENT: 'Equipaggiamento' } as const

// Il retro di una carta: la cornice del suo Tier (magie ed equipaggiamenti usano la Comune) con seme, nome e lore
// nel riquadro. Le misure sono in percentuale della carta, i testi in cqw: funziona a qualsiasi larghezza.
export default function CardBack({ card, sizes = '300px' }: { card: GameCard; sizes?: string }) {
  const frame = cardBackFrame(card.kind === 'CREATURE' ? creatureTier(card.rank) : 1)
  const line = card.kind === 'CREATURE' ? [card.suit && SUIT_NAMES[card.suit], card.subfaction].filter(Boolean).join(' · ') : KIND_NAME[card.kind]

  return (
    <span className="q-retro">
      <img className="q-retro__cornice" src={frame.src} srcSet={frame.srcSet} sizes={sizes} alt="" draggable={false} decoding="async" loading="lazy" />
      <span className="q-retro__testo">
        <span className="q-retro__sopra">{line}</span>
        <span className="q-retro__nome" lang={card.textLocale}>{card.name}</span>
        <span className="q-retro__lore" lang={card.textLocale}>{card.flavour ?? 'Questa carta non ha ancora un testo.'}</span>
      </span>
    </span>
  )
}
