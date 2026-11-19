import { useState } from 'react'
import type { Deck } from '../../types/deck'
import { factions } from '../../data/factions'
import { useDeckStats } from '../../hooks/useDeckStats'
import { GENERIC } from '../../lib/api'
import SuitList from './SuitList'
import DeckActions from './DeckActions'

type Props = {
  deck: Deck | null
  onPlay: () => void
  onEdit: (deck: Deck) => void
  onDelete: (id: string) => Promise<void>
  onSetDefense: (id: string) => Promise<void>
}

// Stato del mazzo in una frase: obsoleto, di difesa, oppure pronto.
function statusText(deck: Deck) {
  if (deck.outdated) return 'Mazzo obsoleto per il pool di carte attuale: modificalo per usarlo di nuovo.'
  if (deck.defense) return 'Mazzo di difesa della sua fazione, pronto per giocare.'
  return 'Mazzo valido, pronto per giocare.'
}

// Pannello a destra con i dettagli del mazzo scelto (pannello q-tela del design system).
export default function DeckDetails({ deck, onPlay, onEdit, onDelete, onSetDefense }: Props) {
  const { stats, failed } = useDeckStats(deck?.id ?? null)
  const [deleting, setDeleting] = useState(false)
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
          <span className="decks-etichetta">Mazzo scelto</span>
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

  return (
    <section className="q-tela deck-details" aria-labelledby="mazzo-scelto">
      <div className="q-tela__in" aria-live="polite">
        <span className="decks-etichetta">{deck.defense ? 'Mazzo di difesa' : 'Mazzo scelto'}</span>
        <span className="deck-details__testa">
          <h2 id="mazzo-scelto" className="deck-details__nome">{deck.name}</h2>
          <span className="deck-details__fazione">{factions[deck.faction].name}</span>
        </span>
        <span className="q-divisorio" />

        <div className="deck-conte">
          <span className="m-conta"><b>{deck.cards}</b><small>/ {deck.maxCards} carte</small></span>
          <span className="m-conta"><b>{count(stats?.creatures)}</b><small>Creature</small></span>
          <span className="m-conta"><b>{count(stats?.spells)}</b><small>Magie / Equip.</small></span>
        </div>

        <div className="deck-semi">
          <span className="decks-etichetta">Creature per seme</span>
          {stats ? (
            <SuitList suits={stats.suits} />
          ) : (
            <p className="deck-empty">{failed ? 'Impossibile caricare le carte del mazzo.' : 'Caricamento…'}</p>
          )}
        </div>

        <p className={`m-regola ${deck.outdated ? 'm-regola--off' : 'm-ok'}`}>{statusText(deck)}</p>
        {!deck.defense && !deck.outdated && (
          // Per giocare serve un mazzo di difesa valido per ogni fazione: si sceglie da qui o dal riepilogo del mazzo.
          <button type="button" className="info-link deck-difesa" onClick={() => makeDefense(deck)} disabled={deleting}>
            Usa come mazzo di difesa della {factions[deck.faction].name}
          </button>
        )}
        {error && <p className="m-regola m-regola--errore" role="alert">{error}</p>}

        <DeckActions
          onPlay={onPlay}
          onEdit={() => onEdit(deck)}
          onDelete={() => remove(deck)}
          canPlay={!deck.outdated}
          // Il mazzo di difesa non si può eliminare (il backend risponde deck.defense_undeletable).
          canDelete={!deck.defense && !deleting}
          deleteHint={deck.defense ? 'Il mazzo di difesa non può essere eliminato' : undefined}
        />
      </div>
    </section>
  )
}
