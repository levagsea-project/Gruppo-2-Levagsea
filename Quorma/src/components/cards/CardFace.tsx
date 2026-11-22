import type { GameCard } from '../../types/deck'
import CreatureCard from './CreatureCard'
import '../../styles/editor.css' // carta "mini" del design system (.q-cg)

const KIND = { SPELL: { letter: 'M', name: 'Magia' }, EQUIPMENT: { letter: 'E', name: 'Equipaggiamento' } } as const

// Il fronte di una carta: le creature con la cornice del Tier, magie ed equipaggiamenti con la carta "mini".
export default function CardFace({ card, sizes }: { card: GameCard; sizes?: string }) {
  if (card.kind === 'CREATURE') return <CreatureCard card={card} sizes={sizes} />
  const kind = KIND[card.kind]
  return (
    <span className="q-cg q-cg--mini card-face">
      <span className="q-cg__campo q-cg__arte e-arte">
        <span className="e-arte__rango">{kind.letter}</span>
      </span>
      <span className="q-cg__campo q-cg__targa">
        <span className="q-cg__nome" lang={card.textLocale}>{card.name}</span>
        <span className="q-cg__sotto">{kind.name}</span>
      </span>
      {/* l'effetto: solo sulle carte larghe almeno 220px (collection.css), sotto non si legge */}
      {!!card.abilities?.length && <span className="q-cg__campo card-face__effetto">{card.abilities.map((a) => a.text).join(' ')}</span>}
    </span>
  )
}
