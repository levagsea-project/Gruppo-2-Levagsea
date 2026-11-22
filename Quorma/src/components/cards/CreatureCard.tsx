import { cardArt, creatureFrame, TIER_NAMES } from '../../data/cardArt'
import { abilityFront } from '../../lib/abilities'
import { SUIT_NAMES, creatureTier, rankLabel } from '../../lib/cards'
import type { GameCard } from '../../types/deck'
import '../../styles/card-frame.css'

type Props = {
  card: GameCard
  className?: string
  sizes?: string // larghezza a cui viene mostrata, per scegliere la cornice da 400 o da 800px
}

const fmt = (n: number | null) => (n === null ? '–' : n.toLocaleString('it-IT'))

// Una creatura con la sua cornice (una per Tier, data/cardArt.ts).
// La cornice è un'immagine con la finestra dell'illustrazione trasparente: l'illustrazione sta sotto,
// rango, nome, seme, sottofazione e ATK/DEF sono testo HTML sopra, nelle caselle della cornice.
// Senza illustrazione, nella finestra compaiono il seme e il rango.
// Le misure sono in percentuale della carta (card-frame.css), quindi funziona a qualsiasi larghezza.
// Sotto i 140px di larghezza il nome nella cornice sarebbe illeggibile: si nasconde e nella sua casella
// compare il seme. Chi mostra la carta così piccola deve scrivere il nome sotto (vedi EditorCard).
export default function CreatureCard({ card, className = '', sizes = '160px' }: Props) {
  const tier = creatureTier(card.rank)
  const frame = creatureFrame(tier)
  const art = cardArt(card.code)
  const suit = card.suit?.toLowerCase()

  return (
    <span className={`q-cf q-cf--t${tier} ${className}`} title={`${card.name} · ${TIER_NAMES[tier]}`}>
      <span className="q-cf__arte">
        {art && <img className="q-cf__img" src={art.src} srcSet={art.srcSet} sizes={sizes} alt={`Illustrazione di ${card.name}`} draggable={false} decoding="async" loading="lazy" />}
        {!art && (
          <span className="q-cf__vuoto" aria-hidden="true">
            {suit && <span className={`deck-seme deck-seme--${suit} q-cf__vuoto-seme`} />}
            <span className="q-cf__vuoto-rango">{rankLabel(card.rank)}</span>
          </span>
        )}
      </span>
      <img className="q-cf__cornice" src={frame.src} srcSet={frame.srcSet} sizes={sizes} alt="" draggable={false} decoding="async" loading="lazy" />

      <span className="q-cf__rango">{rankLabel(card.rank)}</span>
      <span className="q-cf__nome" lang={card.textLocale}>{card.name}</span>
      {/* sulle carte piccole il nome non si legge: al suo posto il seme (card-frame.css), il nome lo mostra chi usa la carta */}
      {suit && <span className={`deck-seme deck-seme--${suit} q-cf__sigillo`} aria-hidden="true" />}
      <span className="q-cf__seme">
        {suit && <span className={`deck-seme deck-seme--${suit}`} role="img" aria-label={SUIT_NAMES[card.suit!]} />}
      </span>
      <span className="q-cf__tipo">{card.subfaction ?? TIER_NAMES[tier]}</span>
      {/* gli effetti nella casella grande: solo sulle carte larghe almeno 220px (card-frame.css), sotto non si leggono.
          La lore sta sul retro; la descrizione completa accanto alla carta (AbilityList) */}
      {!!card.abilities?.length && <span className="q-cf__testo">{abilityFront(card.abilities)}</span>}
      <span className="q-cf__valore q-cf__valore--atk">
        <small>ATK</small>
        {fmt(card.atk)}
      </span>
      <span className="q-cf__valore q-cf__valore--def">
        <small>DEF</small>
        {fmt(card.def)}
      </span>
    </span>
  )
}
