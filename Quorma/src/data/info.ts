// Voci delle liste laterali delle pagine "Informazioni". Cambia i testi qui.
import type { Page } from './navItems'

export const aboutSections = ['Lo studio', 'Il gioco', 'La squadra', 'Cosa ci guida', 'Lavora con noi']

export const faqTopics = [
  { label: 'Tutti', count: 11 },
  { label: 'Account', count: 3 },
  { label: 'Partite', count: 3 },
  { label: 'Mazzi', count: 2 },
  { label: 'Negozio', count: 3 },
]

// Lettere del glossario: `available` = hanno almeno un termine (le altre sono spente).
export const glossaryLetters = [
  { letter: 'A', available: true }, { letter: 'B', available: true }, { letter: 'C', available: true },
  { letter: 'D', available: true }, { letter: 'E', available: true }, { letter: 'F', available: true },
  { letter: 'G' }, { letter: 'H' }, { letter: 'I' }, { letter: 'L' },
  { letter: 'M', available: true }, { letter: 'N' }, { letter: 'O' },
  { letter: 'P', available: true }, { letter: 'Q' }, { letter: 'R', available: true },
  { letter: 'S', available: true }, { letter: 'T', available: true }, { letter: 'U' },
  { letter: 'V', available: true }, { letter: 'Z' },
]

export const contactTabs = ['Account', 'Pagamenti', 'Segnala un problema', 'Altro']

// Le tre pagine legali hanno lo stesso aspetto: cambiano solo titolo e indice.
const baseSections = ['Introduzione', 'Lorem ipsum dolor', 'Consectetur adipiscing', 'Sed do eiusmod']

export const legalPages: Record<
  'privacy' | 'cookie' | 'termini',
  { page: Page; title: string; subtitle: string; sections: string[] }
> = {
  termini: {
    page: 'termini',
    title: 'TERMINI',
    subtitle: 'TERMINI E CONDIZIONI DI SERVIZIO',
    sections: [...baseSections, 'Ut enim ad minim', 'Contatti'],
  },
  privacy: {
    page: 'privacy',
    title: 'PRIVACY',
    subtitle: 'INFORMATIVA SULLA PRIVACY',
    sections: [...baseSections, 'Ut enim ad minim', 'Contatti'],
  },
  cookie: {
    page: 'cookie',
    title: 'COOKIE',
    subtitle: 'INFORMATIVA SUI COOKIE',
    sections: [...baseSections, 'Gestione dei cookie'],
  },
}

export const LAST_UPDATE = 'ULTIMO AGGIORNAMENTO · 1 NOVEMBRE 2026'
