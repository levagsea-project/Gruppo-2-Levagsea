import { useEffect, useState } from 'react'
import type { DeckDetail, DeckStats } from '../types/deck'
import { getDeckStats, statsOf } from '../lib/decks'

// Conteggi del mazzo scelto (creature, magie, semi): la lista dei mazzi non li ha, quindi si legge
// il mazzo intero (GET /decks/{id}). Ogni mazzo si legge una volta sola: le risposte restano qui.
const cache = new Map<string, DeckStats>()

/** Dopo un salvataggio: i conteggi del mazzo vengono dalle sue carte nuove, non dalla lettura precedente. */
export const rememberDeckStats = (deck: DeckDetail) => cache.set(deck.id, statsOf(deck.cardList))

export function useDeckStats(id: string | null) {
  // Ultima risposta arrivata: serve solo a ridisegnare e a sapere se la lettura è fallita.
  const [last, setLast] = useState<{ id: string; failed: boolean } | null>(null)

  useEffect(() => {
    if (!id || cache.has(id)) return
    let current = true // se intanto il giocatore sceglie un altro mazzo, la risposta vecchia si ignora
    getDeckStats(id)
      .then((s) => {
        cache.set(id, s)
        if (current) setLast({ id, failed: false })
      })
      .catch(() => {
        if (current) setLast({ id, failed: true })
      })
    return () => {
      current = false
    }
  }, [id])

  return {
    stats: id ? cache.get(id) ?? null : null,
    failed: !!id && last?.id === id && last.failed,
  }
}
