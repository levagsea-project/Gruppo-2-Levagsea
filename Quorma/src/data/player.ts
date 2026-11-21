import { getSession } from '../lib/auth'
import { loadProfile } from '../lib/profileStore'
import { findAvatar } from './avatars'

// Il giocatore. Il backend per ora restituisce solo il nome (all'accesso): livello, esperienza,
// valute, partite e scambi non esistono ancora, quindi non li mostriamo come se fossero veri
// (PRODUCT.md, principio 4). Quando arriveranno le chiamate del backend, si collegano qui.

/** Iniziali per l'avatar: "mario_rossi" → "MR", "Lupo" → "LU". */
function initialsOf(name: string) {
  const parts = name.split(/[\s_.-]+/).filter(Boolean)
  const letters = parts.length > 1 ? parts[0][0] + parts[1][0] : name.slice(0, 2)
  return letters.toUpperCase()
}

/** Il giocatore che ha fatto l'accesso: nome e immagine scelti in "Modifica profilo", altrimenti il nome dell'account. */
export function currentPlayer() {
  const stored = loadProfile()
  const name = stored.name?.trim() || getSession()?.username?.trim() || 'Giocatore'
  return { name, initials: initialsOf(name), avatar: findAvatar(stored.avatar) }
}

// Le due valute (nomi provvisori). I crediti del backend sono la Valuta 2; la Valuta 1 non esiste ancora (null = "in arrivo").
export const currencies = (credits: number | null): { label: string; value: string | null }[] => [
  { label: 'Valuta 1', value: null },
  { label: 'Valuta 2', value: credits === null ? null : credits.toLocaleString('it-IT') },
]

// Sezioni del gioco che non esistono ancora: le mostriamo come "in arrivo", senza numeri inventati.
export const comingSoon = [
  { id: 'scambi', label: 'Scambi', detail: 'Scambia i doppioni con altri giocatori', icon: 'scambi' },
] as const
