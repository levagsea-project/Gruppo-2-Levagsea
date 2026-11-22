import { useRef, type PointerEvent } from 'react'
import type { OwnedCard } from '../../types/deck'
import { SUIT_NAMES, rankLabel } from '../../lib/cards'
import CardStats from '../cards/CardStats'
import CreatureCard from '../cards/CreatureCard'

export type CardState = 'dentro' | 'libera' | 'bloccata'

type Props = { card: OwnedCard; state: CardState; reason: string; onToggle: () => void; onZoom: () => void }

const KIND_LETTER = { SPELL: 'M', EQUIPMENT: 'E' } as const
const KIND_NAME = { SPELL: 'Magia', EQUIPMENT: 'Equipaggiamento' } as const

const LONG_PRESS_MS = 450
const MOVE_TOLERANCE = 10 // px: oltre, è uno scorrimento della lista e non una pressione lunga

// Una carta della collezione nell'editor. Le creature hanno la cornice del loro Tier (CreatureCard);
// magie ed equipaggiamenti, che non hanno ancora una cornice, usano la carta "mini" del design system con M / E.
// Tocco = aggiungi/togli. Pressione lunga (telefono) o lente = carta ingrandita (CardZoom).
export default function EditorCard({ card, state, reason, onToggle, onZoom }: Props) {
  const creature = card.kind === 'CREATURE'
  const inDeck = state === 'dentro'
  const usable = state !== 'bloccata'
  const what = creature ? `${card.name}, ${rankLabel(card.rank)} di ${SUIT_NAMES[card.suit!]}, ATK ${card.atk}, DEF ${card.def}` : `${card.name}, ${KIND_NAME[card.kind as 'SPELL']}`
  const action = inDeck ? 'nel mazzo, premi per togliere' : usable ? 'premi per aggiungere' : reason

  // ---------- Pressione lunga (solo tocco: col mouse c'è la lente) ----------
  const timer = useRef<number | null>(null)
  const origin = useRef({ x: 0, y: 0 })
  const zoomed = useRef(false) // la pressione lunga ha aperto la carta: il "click" che segue non deve aggiungerla

  const stop = () => {
    if (timer.current !== null) window.clearTimeout(timer.current)
    timer.current = null
  }
  const onPointerDown = (e: PointerEvent) => {
    if (e.pointerType !== 'touch') return
    zoomed.current = false
    origin.current = { x: e.clientX, y: e.clientY }
    stop()
    timer.current = window.setTimeout(() => {
      timer.current = null
      zoomed.current = true
      navigator.vibrate?.(12)
      onZoom()
    }, LONG_PRESS_MS)
  }
  const onPointerMove = (e: PointerEvent) => {
    if (timer.current === null) return
    if (Math.hypot(e.clientX - origin.current.x, e.clientY - origin.current.y) > MOVE_TOLERANCE) stop()
  }
  const onClick = () => {
    if (zoomed.current) {
      zoomed.current = false
      return
    }
    if (usable) onToggle()
  }

  return (
    <div className="e-cella">
      <button
        type="button"
        className={`e-carta e-carta--${state}`}
        aria-pressed={inDeck}
        aria-disabled={!usable}
        aria-label={`${what}: ${action}`}
        onClick={onClick}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={stop}
        onPointerCancel={stop}
        onPointerLeave={stop}
        // il menu del browser (Android) arriva dopo la pressione lunga: se la carta si è aperta, non serve
        onContextMenu={(e) => (zoomed.current || timer.current !== null) && e.preventDefault()}
      >
        {creature ? (
          <CreatureCard card={card} className="e-faccia" sizes="(max-width: 767px) 33vw, 170px" />
        ) : (
          // Magie ed equipaggiamenti non hanno ancora una cornice: carta "mini" del design system
          <span className={`q-cg q-cg--mini e-faccia ${inDeck ? 'q-acceso' : ''}`}>
            <span className="q-cg__campo q-cg__arte e-arte">
              <span className="e-arte__rango">{KIND_LETTER[card.kind as 'SPELL']}</span>
            </span>
            <span className="q-cg__campo q-cg__targa">
              <span className="q-cg__nome" lang={card.textLocale}>{card.name}</span>
              <span className="q-cg__sotto">{KIND_NAME[card.kind as 'SPELL']}</span>
            </span>
          </span>
        )}
        {usable && (
          <span className="e-segno" aria-hidden="true">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
              {inDeck ? <path d="m5 12.5 4.5 4.5L19 7.5" /> : <path d="M12 5v14M5 12h14" />}
            </svg>
          </span>
        )}
        {/* carta piccola: il nome non sta nella cornice e si legge qui (editor.css, @container e-carta) */}
        {creature && (
          <span className="e-nome" aria-hidden="true" lang={card.textLocale}>
            {card.name}
          </span>
        )}
        <CardStats card={card} />
        <span className="e-stato" aria-hidden="true">{inDeck ? 'Nel mazzo' : usable ? 'Aggiungi' : reason}</span>
      </button>

      {/* Lente: apre la carta ingrandita. Fuori dal bottone della carta (un bottone non può contenerne un altro) */}
      <button type="button" className="e-lente" aria-label={`Ingrandisci ${card.name}`} title="Ingrandisci" onClick={onZoom}>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" aria-hidden="true">
          <circle cx="10.5" cy="10.5" r="6" />
          <path d="m15 15 5.5 5.5M10.5 8v5M8 10.5h5" />
        </svg>
      </button>
    </div>
  )
}
