// Saldo del giocatore, letto dal backend (GET /wallet). Resta solo in memoria: nel browser non si salva,
// così non può sopravvivere a un saldo cambiato altrove. null = non ancora letto, oppure lettura non riuscita.
import { useEffect, useSyncExternalStore } from 'react'
import { authedRequest, getSession, isDemoSession } from './auth'

const listeners = new Set<() => void>()
let known: { username: string; credits: number } | null = null
let version = 0 // cresce a ogni saldo ricevuto: una lettura partita prima non può sovrascriverne uno più recente

/** "4.800": in italiano toLocaleString non separa le migliaia sotto 10.000, il design sì. */
export const formatCredits = (n: number) => String(n).replace(/\B(?=(\d{3})+(?!\d))/g, '.')

const read = () => {
  const username = getSession()?.username
  return known && known.username === username ? known.credits : null
}

export function setBalance(credits: number) {
  const username = getSession()?.username
  if (!username) return
  version++
  known = { username, credits }
  listeners.forEach((notify) => notify())
}

/** Rilegge il saldo. Se non riesce resta l'ultimo valore ricevuto in questa sessione. */
export async function refreshBalance() {
  if (!getSession() || isDemoSession()) return
  const startedAt = version
  try {
    const { credits } = await authedRequest<{ credits: number }>('GET', '/wallet')
    if (startedAt === version && Number.isFinite(credits)) setBalance(credits)
  } catch {
    /* la barra mostra il trattino e a decidere un acquisto resta il backend */
  }
}

/** Tiene aggiornato il saldo: a ogni cambio di pagina e quando si torna sulla scheda (premi, scambi, altri dispositivi). */
export function useWalletSync(page: string) {
  useEffect(() => {
    void refreshBalance()
  }, [page])
  useEffect(() => {
    const onVisible = () => {
      if (!document.hidden) void refreshBalance()
    }
    document.addEventListener('visibilitychange', onVisible)
    return () => document.removeEventListener('visibilitychange', onVisible)
  }, [])
}

function subscribe(notify: () => void) {
  listeners.add(notify)
  return () => {
    listeners.delete(notify)
  }
}

/** Il saldo attuale; la pagina si aggiorna da sola quando cambia. */
export const useBalance = () => useSyncExternalStore(subscribe, read, () => null)
