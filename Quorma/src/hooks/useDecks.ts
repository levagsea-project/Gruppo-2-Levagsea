import { useEffect, useState } from 'react'
import type { Deck } from '../types/deck'
import { isDemoSession } from '../lib/auth'
import { deleteDeck as apiDeleteDeck, listDecks } from '../lib/decks'

export type LoadState = 'loading' | 'ready' | 'error'

// La lista dei mazzi del giocatore, letta dal backend (GET /decks, dal più recente).
// In modalità demo il backend non c'è: la lista resta vuota.
export function useDecks() {
  const [decks, setDecks] = useState<Deck[]>([])
  const [state, setState] = useState<LoadState>(() => (isDemoSession() ? 'ready' : 'loading'))
  const [error, setError] = useState('')
  const [selectedId, setSelectedId] = useState<string | null>(null)

  // Ogni lettura incrementa questo numero: l'effetto qui sotto riparte e chiama il backend.
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    if (isDemoSession()) return
    let current = true
    listDecks()
      .then((list) => {
        if (!current) return
        setDecks(list)
        setState('ready')
      })
      .catch((err: unknown) => {
        if (!current) return
        setError(err instanceof Error ? err.message : '')
        setState('error')
      })
    return () => {
      current = false
    }
  }, [attempt])

  // "Riprova" dopo un errore.
  const reload = () => {
    setState('loading')
    setAttempt((n) => n + 1)
  }

  // Mazzo scelto: quello cliccato, oppure il primo, oppure nessuno.
  const selected = decks.find((d) => d.id === selectedId) ?? decks[0] ?? null

  // Elimina sul backend, poi dalla lista. Se il backend rifiuta, l'errore arriva a chi ha chiamato.
  const deleteDeck = async (id: string) => {
    await apiDeleteDeck(id)
    setDecks((old) => old.filter((d) => d.id !== id))
  }

  return { decks, state, error, reload, selected, setSelectedId, deleteDeck }
}
