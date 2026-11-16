import { useState } from 'react'
import type { Deck } from '../types/deck'

// Qui vive la lista dei mazzi. Parte VUOTA: all'inizio c'è solo "Nuovo mazzo".
export function useDecks() {
  const [decks, setDecks] = useState<Deck[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)

  // Mazzo scelto: quello cliccato, oppure il primo, oppure nessuno.
  const selected = decks.find((d) => d.id === selectedId) ?? decks[0] ?? null

  // Quando collegherai "Crea mazzo" userai questa.
  const addDeck = (deck: Deck) => setDecks((old) => [...old, deck])

  // Elimina: tiene tutti i mazzi tranne quello con questo id.
  const deleteDeck = (id: string) => setDecks((old) => old.filter((d) => d.id !== id))

  return { decks, selected, setSelectedId, addDeck, deleteDeck }
}
