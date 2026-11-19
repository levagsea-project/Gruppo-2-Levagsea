import type { OwnedCard } from '../../types/deck'
import { SUIT_NAMES, rankLabel } from '../../lib/cards'

export type CardState = 'dentro' | 'libera' | 'bloccata'

type Props = { card: OwnedCard; state: CardState; reason: string; onToggle: () => void }

const fmt = (n: number | null) => (n === null ? '–' : n.toLocaleString('it-IT'))
const KIND_LETTER = { SPELL: 'M', EQUIPMENT: 'E' } as const
const KIND_NAME = { SPELL: 'Magia', EQUIPMENT: 'Equipaggiamento' } as const

// Una carta della collezione nell'editor (carta "mini" del design system).
// Le carte non hanno ancora illustrazioni: al posto dell'arte c'è il rango con il seme (creature) o M / E.
export default function EditorCard({ card, state, reason, onToggle }: Props) {
  const creature = card.kind === 'CREATURE'
  const inDeck = state === 'dentro'
  const usable = state !== 'bloccata'
  const what = creature ? `${card.name}, ${rankLabel(card.rank)} di ${SUIT_NAMES[card.suit!]}` : `${card.name}, ${KIND_NAME[card.kind as 'SPELL']}`
  const action = inDeck ? 'nel mazzo, premi per togliere' : usable ? 'premi per aggiungere' : reason

  return (
    <button
      type="button"
      className={`e-carta e-carta--${state}`}
      aria-pressed={inDeck}
      aria-disabled={!usable}
      aria-label={`${what}: ${action}`}
      onClick={() => usable && onToggle()}
    >
      <span className={`q-cg q-cg--mini ${inDeck ? 'q-acceso' : ''}`}>
        <span className="q-cg__campo q-cg__arte e-arte">
          {creature ? (
            <>
              <span className={`deck-seme deck-seme--${card.suit!.toLowerCase()} e-arte__seme`} aria-hidden="true" />
              <span className="e-arte__rango">{rankLabel(card.rank)}</span>
              <span className="e-arte__valori">
                {fmt(card.atk)} / {fmt(card.def)}
              </span>
            </>
          ) : (
            <span className="e-arte__rango">{KIND_LETTER[card.kind as 'SPELL']}</span>
          )}
        </span>
        <span className="q-cg__campo q-cg__targa">
          <span className="q-cg__nome">{card.name}</span>
          <span className="q-cg__sotto">{creature ? card.subfaction : KIND_NAME[card.kind as 'SPELL']}</span>
        </span>
      </span>
      {usable && (
        <span className="e-segno" aria-hidden="true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
            {inDeck ? <path d="m5 12.5 4.5 4.5L19 7.5" /> : <path d="M12 5v14M5 12h14" />}
          </svg>
        </span>
      )}
      <span className="e-stato" aria-hidden="true">{inDeck ? 'Nel mazzo' : usable ? 'Aggiungi' : reason}</span>
    </button>
  )
}
