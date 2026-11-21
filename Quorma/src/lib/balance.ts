// Saldo del giocatore. Il backend non ha una chiamata per leggerlo: lo comunica solo nella risposta di un acquisto
// (creditsAfter). Qui si ricorda l'ultimo valore ricevuto, per account e in questo browser.
// null = nessun acquisto fatto da questo browser, quindi saldo non noto.
import { useSyncExternalStore } from 'react'
import { getSession } from './auth'

const KEY = () => `quorma.saldo.${getSession()?.username ?? 'ospite'}`
const listeners = new Set<() => void>()

function read(): number | null {
  try {
    const raw = localStorage.getItem(KEY())
    const value = raw === null ? NaN : Number(raw)
    return Number.isFinite(value) ? value : null
  } catch {
    return null
  }
}

export function setBalance(value: number) {
  try {
    localStorage.setItem(KEY(), String(value))
  } catch {
    /* browser senza salvataggio: il saldo tornerà noto al prossimo acquisto */
  }
  listeners.forEach((notify) => notify())
}

function subscribe(notify: () => void) {
  listeners.add(notify)
  return () => {
    listeners.delete(notify)
  }
}

/** L'ultimo saldo noto; la pagina si aggiorna da sola dopo ogni acquisto. */
export const useBalance = () => useSyncExternalStore(subscribe, read, () => null)
