import { getSession } from './auth'
import type { Match } from '../data/profile'

// L'archivio delle partite. Il backend non le ha ancora: stanno nel browser (localStorage),
// una lista per ogni account. Statistiche e cronologia del profilo si calcolano da qui.
const MAX_MATCHES = 200
const key = () => `quorma.matches.${getSession()?.username ?? 'ospite'}`

/** Tutte le partite, dalla più recente. */
export function loadMatches(): Match[] {
  try {
    const raw = localStorage.getItem(key())
    const list = raw ? JSON.parse(raw) : []
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

/** Aggiunge una partita finita. Il gioco vero chiamerà questa funzione a fine partita. true = salvata. */
export function recordMatch(match: Match): boolean {
  try {
    localStorage.setItem(key(), JSON.stringify([match, ...loadMatches()].slice(0, MAX_MATCHES)))
    return true
  } catch {
    return false
  }
}

const day = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()

/** "Oggi, 18:40" · "Ieri, 22:12" · "12 nov, 20:47" */
export function formatWhen(timestamp: number): string {
  const d = new Date(timestamp)
  const time = d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
  const daysAgo = Math.round((day(new Date()) - day(d)) / 86_400_000)
  if (daysAgo === 0) return `Oggi, ${time}`
  if (daysAgo === 1) return `Ieri, ${time}`
  return `${d.getDate()} ${d.toLocaleDateString('it-IT', { month: 'short' }).replace('.', '')}, ${time}`
}
