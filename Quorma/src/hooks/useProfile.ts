import { isDemoSession } from '../lib/auth'
import { demoMatches, demoOverview } from '../data/profile'
import type { Match, Overview } from '../data/profile'

// Statistiche e partite del giocatore. Il backend non le ha ancora:
// accesso vero → nessun dato (overview null, partite vuote); accesso demo → dati di prova.
// Quando arriveranno le chiamate, si collegano qui e il resto della pagina non cambia.
export function useProfile(): { overview: Overview | null; matches: Match[] } {
  return isDemoSession() ? { overview: demoOverview, matches: demoMatches } : { overview: null, matches: [] }
}
