import { useState } from 'react'
import type { Deck } from '../../types/deck'
import { factions } from '../../data/factions'
import { useDeckStats } from '../../hooks/useDeckStats'
import { GENERIC } from '../../lib/api'
import { deckErrorMessages } from '../../lib/decks'
import DeckActions from './DeckActions'

type Props = {
  deck: Deck | null
  onPlay: () => void
  onEdit: (deck: Deck) => void
  onDuplicate: (deck: Deck) => Promise<void>
  onDelete: (id: string) => Promise<void>
  onSetDefense: (id: string) => Promise<void>
}

// Stato del mazzo in una frase: obsoleto, incompleto, di difesa, oppure pronto.
function statusText(deck: Deck) {
  if (deck.outdated) return 'Mazzo obsoleto per il pool di carte attuale: modificalo per usarlo di nuovo.'
  if (deck.cards < deck.maxCards) return `Mancano ${deck.maxCards - deck.cards} carte.`
  if (deck.defense) return 'Mazzo di difesa della sua fazione, pronto per giocare.'
  return 'Mazzo valido, pronto per giocare.'
}

// Pannello a destra con i dettagli del mazzo scelto, come nel design: nome e fazione, tre conteggi, stato, azioni.
export default function DeckDetails({ deck, onPlay, onEdit, onDuplicate, onDelete, onSetDefense }: Props) {
  const { stats, failed } = useDeckStats(deck?.id ?? null)
  const [deleting, setDeleting] = useState(false)
  const [duplicating, setDuplicating] = useState(false)
  const [error, setError] = useState('')
  const [shownFor, setShownFor] = useState(deck?.id)

  // Cambiando mazzo, l'errore del mazzo precedente non vale più.
  if (shownFor !== deck?.id) {
    setShownFor(deck?.id)
    setError('')
  }

  if (!deck) {
    return (
      <section className="q-tela deck-details" aria-label="Mazzo scelto">
        <div className="q-tela__in">
          <p className="deck-empty">Nessun mazzo creato. Premi "Nuovo mazzo" per iniziare.</p>
        </div>
      </section>
    )
  }

  async function remove(d: Deck) {
    if (!window.confirm(`Eliminare "${d.name}"? Non si può annullare.`)) return
    setDeleting(true)
    setError('')
    try {
      await onDelete(d.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : GENERIC)
    } finally {
      setDeleting(false)
    }
  }

  async function duplicate(d: Deck) {
    setDuplicating(true)
    setError('')
    try {
      await onDuplicate(d)
    } catch (err) {
      setError(deckErrorMessages(err).join(' '))
    } finally {
      setDuplicating(false)
    }
  }

  async function makeDefense(d: Deck) {
    setDeleting(true)
    setError('')
    try {
      await onSetDefense(d.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : GENERIC)
    } finally {
      setDeleting(false)
    }
  }

  // "–" finché i conteggi non arrivano dal backend.
  const count = (n: number | undefined) => (n === undefined ? '–' : n)
  const incomplete = deck.cards < deck.maxCards

  return (
    <section className="q-tela deck-details" aria-labelledby="mazzo-scelto">
      <div className="q-tela__in" aria-live="polite">
        <span className="deck-details__testa">
          <h2 id="mazzo-scelto" className="deck-details__nome" title={deck.name}>{deck.name}</h2>
          <span className="deck-details__fazione">{factions[deck.faction].name}</span>
        </span>

        <div className="deck-conte">
          <span className="m-conta"><b>{deck.cards}</b><small>Carte</small></span>
          <span className="m-conta"><b>{count(stats?.creatures)}</b><small>Creature</small></span>
          <span className="m-conta"><b>{count(stats?.spells)}</b><small>Magie</small></span>
        </div>
        {failed && <p className="deck-empty">Impossibile caricare le carte del mazzo.</p>}

        <p className={`m-regola ${deck.outdated || incomplete ? 'm-regola--off' : 'm-ok'}`}>{statusText(deck)}</p>
        {!deck.defense && !deck.outdated && !incomplete && (
          // Per giocare serve un mazzo di difesa valido per ogni fazione: si sceglie da qui o dal riepilogo del mazzo.
          <button type="button" className="info-link deck-difesa" onClick={() => makeDefense(deck)} disabled={deleting}>
            Usa come mazzo di difesa della {factions[deck.faction].name}
          </button>
        )}
        {error && <p className="m-regola m-regola--errore" role="alert">{error}</p>}

        <DeckActions
          onPlay={onPlay}
          onEdit={() => onEdit(deck)}
          onDuplicate={() => duplicate(deck)}
          onDelete={() => remove(deck)}
          canPlay={!deck.outdated && !incomplete}
          editLabel={incomplete ? 'Completa il mazzo' : 'Modifica'}
          duplicating={duplicating}
          busy={deleting || duplicating}
          // Il mazzo di difesa non si può eliminare (il backend risponde deck.defense_undeletable).
          canDelete={!deck.defense && !deleting && !duplicating}
          deleteHint={deck.defense ? 'Il mazzo di difesa non può essere eliminato' : undefined}
        />
      </div>
    </section>
  )
}
