import { getSession } from '../lib/auth'

// Il giocatore. Il backend per ora restituisce solo il nome (all'accesso): livello, esperienza,
// valute, partite e scambi non esistono ancora, quindi non li mostriamo come se fossero veri
// (PRODUCT.md, principio 4). Quando arriveranno le chiamate del backend, si collegano qui.

/** Iniziali per l'avatar: "mario_rossi" → "MR", "Lupo" → "LU". */
function initialsOf(name: string) {
  const parts = name.split(/[\s_.-]+/).filter(Boolean)
  const letters = parts.length > 1 ? parts[0][0] + parts[1][0] : name.slice(0, 2)
  return letters.toUpperCase()
}

/** Il giocatore che ha fatto l'accesso (nome dalla sessione). */
export function currentPlayer() {
  const name = getSession()?.username?.trim() || 'Giocatore'
  return { name, initials: initialsOf(name) }
}

// Le due valute: nomi provvisori, saldo non ancora disponibile dal backend (null = "in arrivo").
export const currencies: { label: string; value: string | null }[] = [
  { label: 'Valuta 1', value: null },
  { label: 'Valuta 2', value: null },
]

// Sezioni del gioco che non esistono ancora: le mostriamo come "in arrivo", senza numeri inventati.
export const comingSoon = [
  { id: 'collezione', label: 'Collezione e buste', detail: 'Apri le buste e sfoglia le tue carte', icon: 'collezione' },
  { id: 'scambi', label: 'Scambi', detail: 'Scambia i doppioni con altri giocatori', icon: 'scambi' },
  { id: 'sfide', label: 'Sfide', detail: 'Sfida gli altri giocatori e riscuoti le ricompense', icon: 'sfide' },
] as const
