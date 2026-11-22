import { useEffect, useRef, type MouseEvent, type PointerEvent } from 'react'
import { TIER_NAMES } from '../../data/cardArt'
import { SUIT_NAMES, creatureTier, rankLabel } from '../../lib/cards'
import type { OwnedCard } from '../../types/deck'
import AbilityList from '../cards/AbilityList'
import CreatureCard from '../cards/CreatureCard'
import type { CardState } from './EditorCard'

type Props = { card: OwnedCard; state: CardState; reason: string; onToggle: () => void; onClose: () => void }

const KIND_LETTER = { SPELL: 'M', EQUIPMENT: 'E' } as const
const KIND_NAME = { SPELL: 'Magia', EQUIPMENT: 'Equipaggiamento' } as const

// La carta ingrandita: si apre con la pressione lunga sulla carta (telefono) o con la lente (EditorCard).
// Mostra la carta a grandezza piena, quello che sulla carta piccola non si legge, e il bottone per aggiungerla
// o toglierla dal mazzo: lo stato si aggiorna senza chiudere la finestra.
// Usa <dialog> del browser: blocca il resto, si chiude con Esc o toccando fuori, tiene il focus dentro.
export default function CardZoom({ card, state, reason, onToggle, onClose }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const creature = card.kind === 'CREATURE'

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null
    const dialog = dialogRef.current
    if (dialog && !dialog.open) dialog.showModal()
    // chiusa la finestra, il focus torna da dove era partito (la carta o la sua lente)
    return () => opener?.focus?.()
  }, [])

  // Tocco sullo sfondo = chiudi, ma solo se anche la pressione è iniziata sullo sfondo: alzando il dito dopo
  // la pressione lunga che ha aperto la carta, il click arriva qui e non deve richiuderla subito.
  const downOnBackdrop = useRef(false)
  const onPointerDown = (e: PointerEvent<HTMLDialogElement>) => {
    downOnBackdrop.current = e.target === dialogRef.current
  }
  const onBackdrop = (e: MouseEvent<HTMLDialogElement>) => {
    if (e.target === dialogRef.current && downOnBackdrop.current) onClose()
    downOnBackdrop.current = false
  }

  const kindLine = creature
    ? `${rankLabel(card.rank)} di ${SUIT_NAMES[card.suit!]} · ${TIER_NAMES[creatureTier(card.rank)]}`
    : KIND_NAME[card.kind as 'SPELL']

  return (
    <dialog
      ref={dialogRef}
      className="e-zoom"
      aria-labelledby="e-zoom-nome"
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      onPointerDown={onPointerDown}
      onClick={onBackdrop}
    >
      <button type="button" className="e-zoom__chiudi" aria-label="Chiudi" onClick={onClose}>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
          <path d="M6 6l12 12M18 6 6 18" />
        </svg>
      </button>

      <div className="e-zoom__carta">
        {creature ? (
          <CreatureCard card={card} sizes="(max-width: 767px) 80vw, 380px" interactive />
        ) : (
          <span className="q-cg e-zoom__mini">
            <span className="q-cg__campo q-cg__arte e-arte">
              <span className="e-arte__rango">{KIND_LETTER[card.kind as 'SPELL']}</span>
            </span>
            <span className="q-cg__campo q-cg__targa">
              <span className="q-cg__nome">{card.name}</span>
              <span className="q-cg__sotto">{KIND_NAME[card.kind as 'SPELL']}</span>
            </span>
          </span>
        )}
      </div>

      <div className="e-zoom__info">
        <h2 id="e-zoom-nome" lang={card.textLocale}>{card.name}</h2>
        <p className="e-zoom__tipo">
          {kindLine}
          {creature && card.subfaction && <span> · {card.subfaction}</span>}
        </p>
        <AbilityList card={card} />
        {card.flavour && (
          <blockquote className="e-zoom__colore" lang={card.textLocale}>
            {card.flavour}
          </blockquote>
        )}
        {card.sets.length > 0 && <p className="e-zoom__set">{card.sets.map((x) => x.name).join(' · ')}</p>}
        {state === 'bloccata' ? (
          <p className="e-zoom__motivo">{reason}</p>
        ) : (
          <button type="button" className={`q-btn ${state === 'dentro' ? 'q-btn--acciaio' : 'q-btn--argento'}`} onClick={onToggle}>
            <span className="q-btn__in">{state === 'dentro' ? 'Togli dal mazzo' : 'Aggiungi al mazzo'}</span>
          </button>
        )}
      </div>
    </dialog>
  )
}
