import { loadMatches } from '../lib/matches'
import { computeOverview } from '../lib/stats'

// Partite e statistiche del giocatore, lette dall'archivio e ricalcolate a ogni apertura del profilo.
// Quando il backend avrà le partite, si cambia solo loadMatches() in lib/matches.ts.
export function useProfile() {
  const matches = loadMatches()
  return { matches, overview: computeOverview(matches) }
}
