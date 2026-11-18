// Dati di prova del giocatore e del suo account, finché non arrivano dal backend.
// Li usano la barra laterale (desktop), la barra in alto e la Home (mobile).
export const player = { name: 'Giocatore 01', initials: 'G1', level: 12, xp: 1280, xpNext: 2000 }

export const currencies = [
  { label: 'Valuta 1', value: '12.500' },
  { label: 'Valuta 2', value: '250' },
]

// Home mobile: cose in attesa e ultima partita.
export const pending = [
  { id: 'buste', label: 'Buste da aprire', detail: '5 buste nella tua collezione', icon: 'collezione' },
  { id: 'scambi', label: 'Scambi', detail: '3 nuove proposte dai tuoi amici', icon: 'scambi' },
  { id: 'sfide', label: 'Sfide', detail: '350 Valuta 1 da riscuotere', icon: 'sfide' },
] as const

export const lastMatch = { result: 'Vittoria', opponent: 'Giocatore 27', detail: 'Classificata · Mazzo 02 · 14 turni' }
